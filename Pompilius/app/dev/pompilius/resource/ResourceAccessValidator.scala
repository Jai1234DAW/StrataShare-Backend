package dev.pompilius.resource

import dev.pompilius.resource.domain.exceptions.ResourceNotAllowedException
import dev.pompilius.resource.domain.{Resource, ResourceId, ResourceUserRepository, ResourceUserType}
import dev.pompilius.shared.domain.{Pagination, Visibility}
import dev.pompilius.users.domain.UserId

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResourceAccessValidator @Inject() (
    userResourceRepository: ResourceUserRepository
)(implicit ec: ExecutionContext) {

  def verifyOwnership(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    userResourceRepository.findByUserAndType(userId, ResourceUserType.OWNER, Pagination.all).map { resourceIds =>
      if (resourceIds.contains(resourceId)) ()
      else throw new ResourceNotAllowedException("You don't have permission to get or update this resource")
    }

    isResourceActive(resourceId, userId).map {
      case true => ()
      case false => throw new ResourceNotAllowedException("You don't have permission to get or update this resource")
    }

  }

  def validateAccess(resource: Resource, userId: UserId): Future[Unit] = {
    resource.visibility match {
      case Visibility.PUBLIC =>
        Future.successful(())

      case Visibility.PRIVATE =>
        // PRIVATE: solo OWNER, PURCHASED o ACCEPTED_AS_PAYMENT
        userResourceRepository.findByResourceAndUser(resource.id, userId).map {
          case Some(resourceUser)
              if resourceUser.resourceUserType == ResourceUserType.OWNER ||
                resourceUser.resourceUserType == ResourceUserType.PURCHASED ||
                resourceUser.resourceUserType == ResourceUserType.ACCEPTED_AS_PAYMENT =>
            ()
          case _ => throw new ResourceNotAllowedException("You don't have access to this private resource")
        }
    }
  }

 private def isResourceActive(resourceId: ResourceId, userId: UserId): Future[Boolean] = {
    userResourceRepository.findByResourceAndUser(resourceId, userId).map {
      case Some(resourceUser) => !resourceUser.deleted
      case None               => false
    }
  }
}
