package dev.pompilius.resource.infrastructure

import dev.pompilius.resource.domain.exceptions.ResourceNotAllowedException
import dev.pompilius.resource.domain.{ResourceId, ResourceUserRepository, ResourceUserType}
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.users.domain.UserId

import scala.concurrent.{Future, ExecutionContext}

trait ResourceAccessValidator extends {

  private[this] var _resourceUserRepository: ResourceUserRepository = _

  @SuppressWarnings(Array("NullParameter"))
  def resourceUserRepository: ResourceUserRepository = {
    if (_resourceUserRepository != null) _resourceUserRepository
    else {
      throw new NoSuchElementException("ResourceUserRepository is not initialized")
    }
  }

  def verifyOwnership(resourceId: ResourceId, userId: UserId)(implicit ec:ExecutionContext): Future[Unit] = {
    resourceUserRepository.findByUserAndType(userId, ResourceUserType.OWNER, Pagination.all).map { resourceIds =>
      if (resourceIds.contains(resourceId)) ()
      else throw new ResourceNotAllowedException("You don't have permission to get or update this resource")
    }

    isResourceActive(resourceId, userId).map {
      case true  => ()
      case false => throw new ResourceNotAllowedException("You don't have permission to get or update this resource")
    }

  }

  private def isResourceActive(resourceId: ResourceId, userId: UserId)(implicit ec:ExecutionContext): Future[Boolean] = {
    resourceUserRepository.findByResourceAndUser(resourceId, userId).map {
      case Some(resourceUser) => !resourceUser.deleted
      case None               => false
    }
  }
}
