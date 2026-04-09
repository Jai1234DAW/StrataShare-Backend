package dev.pompilius.transaction.infrastructure

import com.google.inject.ImplementedBy
import dev.pompilius.Strings.isPublic
import dev.pompilius.barter.domain.{BarterData, BarterNotAllowException}
import dev.pompilius.resource.domain._
import dev.pompilius.resource.domain.exceptions.{ResourceNotAllowedException, ResourceNotFoundException}
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.shared.domain.exceptions.ForbiddenException
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.domain.Visibility.PUBLIC
import dev.pompilius.transaction.domain.{TransactionFilter, TransactionRepository, TransactionStatus}
import dev.pompilius.users.domain.{User, UserId}
import dev.pompilius.users.domain.exceptions.UserNotFoundException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TransactionValidatorImpl])
trait TransactionValidator {
  def verifyNotPendingTransaction(resourceId: ResourceId, userId: UserId): Future[Unit]
  def isAbleToBarter(resourceId: ResourceId, buyer: User, offeredResourceId: ResourceId): Future[BarterData]
}

@Singleton
class TransactionValidatorImpl @Inject() (
    transactionRepository: TransactionRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceAccessValidator: ResourceAccessValidator
)(implicit ec: ExecutionContext)
    extends TransactionValidator {

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
      Future.failed(
        new BarterNotAllowException("This resource is public, does not require a barter"))
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

    _ <-validateResourceIsNotPublic(requestedResource)

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

      _ <-validateResourceIsNotPublic(offeredResource)

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
}
