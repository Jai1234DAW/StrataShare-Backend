package dev.pompilius.transaction.infrastructure

import com.google.inject.ImplementedBy
import dev.pompilius.barter.domain.exception.BarterNotAllowException
import dev.pompilius.barter.domain.{Barter, BarterData}
import dev.pompilius.resource.domain._
import dev.pompilius.resource.domain.exceptions.{ResourceNotAllowedException, ResourceNotFoundException}
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.shared.domain.Visibility.PUBLIC
import dev.pompilius.shared.domain.exceptions.ForbiddenException
import dev.pompilius.shared.domain.{Clock, Pagination}
import dev.pompilius.transaction.domain.{Transaction, TransactionFilter, TransactionRepository, TransactionStatus}
import dev.pompilius.users.domain.exceptions.UserNotFoundException
import dev.pompilius.users.domain.{User, UserId}
import org.apache.pekko.Done

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TransactionServImpl])
trait TransactionService {
  def verifyNotPendingTransaction(resourceId: ResourceId, userId: UserId): Future[Unit]
  def isAbleToBarter(resourceId: ResourceId, buyer: User, offeredResourceId: ResourceId): Future[BarterData]
  def transactionTrans(transaction: Transaction, barter: Barter): Future[Done]
}

@Singleton
class TransactionServImpl @Inject() (
    transactionRepository: TransactionRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceAccessValidator: ResourceAccessValidator,
    clock: Clock
)(implicit ec: ExecutionContext)
    extends TransactionService {

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
        new BarterNotAllowException("You cannot propose a barter for your own resource")
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
    resourceUserRepository
      .save(
        ResourceUser(
          resourceId = transaction.resourceId,
          userId = transaction.buyerId,
          resourceUserType = ResourceUserType.PURCHASED,
          created = clock.now
        )
      )

    resourceUserRepository
      .save(
        ResourceUser(
          resourceId = barter.offeredResourceId,
          userId = transaction.sellerId,
          resourceUserType = ResourceUserType.ACCEPTED_AS_PAYMENT,
          created = clock.now
        )
      )

    transactionRepository.updateStatus(transaction.id, TransactionStatus.COMPLETED)
  }
}
