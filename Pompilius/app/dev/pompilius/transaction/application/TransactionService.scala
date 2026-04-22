package dev.pompilius.transaction.application

import com.google.inject.ImplementedBy
import dev.pompilius.badge.application.BadgeService
import dev.pompilius.barter.domain.exception.BarterNotAllowException
import dev.pompilius.barter.domain.{Barter, BarterData}
import dev.pompilius.event.domain.EventU
import dev.pompilius.resource.domain._
import dev.pompilius.resource.domain.exceptions.{ResourceNotAllowedException, ResourceNotFoundException}
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.shared.domain.Visibility.PUBLIC
import dev.pompilius.shared.domain.exceptions.ForbiddenException
import dev.pompilius.shared.domain.{Clock, Pagination}
import dev.pompilius.transaction.domain.{Transaction, TransactionFilter, TransactionRepository, TransactionStatus}
import dev.pompilius.users.domain.exceptions.UserNotFoundException
import dev.pompilius.users.domain.{User, UserId, UserRepository}
import org.apache.pekko.Done
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TransactionServImpl])
trait TransactionService {
  def verifyNotPendingTransaction(resourceId: ResourceId, userId: UserId): Future[Unit]
  def isAbleToBarter(resourceId: ResourceId, buyer: User, offeredResourceId: ResourceId): Future[BarterData]
  def isAbleToPurchase(resourceId: ResourceId, buyer: User): Future[(Resource, User)]
  def transactionTrans(transaction: Transaction, barter: Barter): Future[Done]
}

@Singleton
class TransactionServImpl @Inject() (
    transactionRepository: TransactionRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceAccessValidator: ResourceAccessValidator,
    userRepository: UserRepository,
    badgeService: BadgeService,
    clock: Clock
)(implicit ec: ExecutionContext)
    extends TransactionService {

  private val logger = Logger(this.getClass)

  override def verifyNotPendingTransaction(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    val transactionFilter = TransactionFilter(
      resourceId = Some(resourceId),
      buyerId = Some(userId),
      transactionStatus = Some(TransactionStatus.PENDING)
    )

    transactionRepository.find(transactionFilter, Pagination(1, Some(1))).map { transactions =>
      if (transactions.nonEmpty) {
        throw new ForbiddenException("You already have a pending transaction for this resource")
      }
    }
  }

  private def validateSameResource(requestedResourceId: ResourceId, offeredResourceId: ResourceId): Future[Unit] = {
    if (requestedResourceId == offeredResourceId) {
      Future.failed(
        new ResourceNotAllowedException("You cannot offer the same resource you are requesting")
      )
    } else {
      Future.successful(())
    }
  }

  private def validateNotSelfTransaction(ownerId: UserId, buyerId: UserId): Future[Unit] = {
    if (ownerId == buyerId) {
      Future.failed(
        new ResourceNotAllowedException("You cannot transact with your own resources")
      )
    } else {
      Future.successful(())
    }
  }

  private def validateResourceIsNotPublic(resource: Resource): Future[Unit] = {
    if (resource.visibility == PUBLIC) {
      Future.failed(new BarterNotAllowException("This resource is public, does not require a barter"))
    } else {
      Future.unit
    }
  }

  override def isAbleToBarter(
      resourceId: ResourceId,
      buyer: User,
      offeredResourceId: ResourceId
  ): Future[BarterData] = {
    for {

      //Primero validaciones sobre el recurso solicitado
      requestedResource <-
        resourceRepository
          .findById(resourceId)
          .map(_.getOrElse(throw new ResourceNotFoundException(s"ResourceId $resourceId not found")))

      //Aquí ya se valida que el usuario dueño del recurso esté activo.
      owner <-
        resourceUserRepository
          .findOwnerByResource(resourceId)
          .map(_.getOrElse(throw new UserNotFoundException(s"Owner of resource $resourceId not found")))

      _ <- resourceAccessValidator.validateResourceIsActive(resourceId, owner.id)

      _ <- validateResourceIsNotPublic(requestedResource)

      //Recurso ofrecido
      offeredResource <-
        resourceRepository
          .findById(offeredResourceId)
          .map(
            _.getOrElse(throw new ResourceNotFoundException(s"Offered resource with id $offeredResourceId not found"))
          )

      _ <- resourceAccessValidator.verifyOwnership(offeredResourceId, buyer.id)

      _ <- validateSameResource(resourceId, offeredResourceId)

      _ <- validateNotSelfTransaction(owner.id, buyer.id)

      _ <- validateResourceIsNotPublic(offeredResource)

      _ <- resourceAccessValidator.validateAlreadyHaveAccess(resourceId, buyer.id)

      _ <- verifyNotPendingTransaction(resourceId, buyer.id)

      data = BarterData(
        requestedResource = requestedResource,
        offeredResource = offeredResource,
        buyer = buyer,
        seller = owner
      )
    } yield data
  }

  override def transactionTrans(transaction: Transaction, barter: Barter): Future[Done] = {
    for {
      // Guardar acceso al recurso solicitado para el comprador
      _ <- resourceUserRepository.save(
        ResourceUser(
          resourceId = transaction.resourceId,
          userId = transaction.buyerId,
          resourceUserType = ResourceUserType.PURCHASED,
          created = clock.now
        )
      )

      // Guardar acceso al recurso ofrecido para el vendedor
      _ <- resourceUserRepository.save(
        ResourceUser(
          resourceId = barter.offeredResourceId,
          userId = transaction.sellerId,
          resourceUserType = ResourceUserType.ACCEPTED_AS_PAYMENT,
          created = clock.now
        )
      )

      // Actualizar el estado de la transacción a COMPLETADO
      _ <- transactionRepository.updateStatus(transaction.id, TransactionStatus.COMPLETED)

      // Registrar evento y verificar badges para el comprador (quien propuso el trueque)
      buyerBadges <- badgeService.registerEventAndCheckBadges(transaction.buyerId, EventU.BARTER_COMPLETED)

      // Registrar evento y verificar badges para el vendedor (quien aceptó el trueque)
      sellerBadges <- badgeService.registerEventAndCheckBadges(transaction.sellerId, EventU.BARTER_COMPLETED)

      _ = if (buyerBadges.nonEmpty) {
        logger.info(
          s"Buyer ${transaction.buyerId.id} earned ${buyerBadges.length} badge(s) after barter: ${buyerBadges.map(_.name).mkString(", ")}"
        )
      }

      _ = if (sellerBadges.nonEmpty) {
        logger.info(
          s"Seller ${transaction.sellerId.id} earned ${sellerBadges.length} badge(s) after barter: ${sellerBadges.map(_.name).mkString(", ")}"
        )
      }

    } yield Done
  }

  override def isAbleToPurchase(resourceId: ResourceId, buyer: User): Future[(Resource, User)] = {
    for {
      // Verificar que el recurso existe
      resource <-
        resourceRepository
          .findById(resourceId)
          .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $resourceId not found")))

      // Verificar que el recurso es PRIVADO (no se compran recursos públicos)
      _ <- validateResourceIsNotPublic(resource)

      // Obtener el vendedor (owner del recurso), aquí mismo se valida que está activo
      seller <-
        resourceUserRepository
          .findOwnerByResource(resourceId)
          .map(_.getOrElse(throw new UserNotFoundException(s"Owner of resource $resourceId not found")))

      seller <-
        userRepository
          .findById(seller.id)
          .map(_.getOrElse(throw new UserNotFoundException(s"Seller user $seller.id not found")))

      // Para validar que el vendedor está habilitado
      _ = if (!seller.enabled) {
        throw new ResourceNotAllowedException("The owner of this resource is no longer active")
      }

      // Validar que el recurso está activo
      _ <- resourceAccessValidator.validateResourceIsActive(resourceId, seller.id)

      // Validar que el comprador no es el vendedor
      _ <- validateNotSelfTransaction(seller.id, buyer.id)

      // Validar que el comprador no tiene ya acceso
      _ <- resourceAccessValidator.validateAlreadyHaveAccess(resourceId, buyer.id)

      // Validar que no tiene transacciones pendientes
      _ <- verifyNotPendingTransaction(resourceId, buyer.id)

    } yield (resource, seller)
  }
}
