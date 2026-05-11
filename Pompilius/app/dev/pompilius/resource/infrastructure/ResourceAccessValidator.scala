package dev.pompilius.resource.infrastructure

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.ResourceUserType.BARTERED
import dev.pompilius.resource.domain.exceptions.{ResourceNotAllowedException, ResourceNotFoundException}
import dev.pompilius.resource.domain._
import dev.pompilius.shared.domain.exceptions.ForbiddenException
import dev.pompilius.shared.domain.{Pagination, Visibility}
import dev.pompilius.users.domain.UserId

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ResourceAccessValidatorImpl])
trait ResourceAccessValidator {

  def verifyOwnership(resourceId: ResourceId, userId: UserId): Future[Unit]
  def validateAccess(resourceId: ResourceId, userId: UserId): Future[Unit]
  def getAccessLevel(resourceId: ResourceId, userId: UserId): Future[ResourceAccessLevel]
  def validateResourceIsActive(resourceId: ResourceId, userId: UserId): Future[Unit]
  def validateAlreadyHaveAccess(resourceId: ResourceId, userId: UserId): Future[Unit]
}

@Singleton
class ResourceAccessValidatorImpl @Inject() (
    resourceUserRepository: ResourceUserRepository,
    resourceRepository: ResourceRepository
)(implicit ec: ExecutionContext)
    extends ResourceAccessValidator {

  def verifyOwnership(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    resourceUserRepository
      .findByUserAndType(userId, ResourceUserType.OWNER, Pagination.all)
      .flatMap { resources =>
        val ownsResource = resources.exists(_.id == resourceId.id)

        if (ownsResource) {
          validateResourceIsActive(resourceId, userId)
        } else {
          Future.failed(new ForbiddenException("You don't have ownership of this resource"))
        }
      }
  }

  def validateResourceIsActive(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    resourceUserRepository.findByResourceAndUser(resourceId, userId).flatMap {
      case Some(ru) if !ru.deleted => Future.unit
      case _ =>
        Future.failed(
          new ResourceNotAllowedException("The requested resource is blocked for purchases")
        )
    }
  }
  def validateAccess(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    resourceRepository.findById(resourceId).flatMap {
      case Some(resource) =>
        resource.visibility match {
          // Si es PÚBLICO → Todos ven datos completos, sin restricciones
          case Visibility.PUBLIC =>
            Future.successful(())

          // Si es PRIVADO → Verificar tipo de acceso del usuario
          case Visibility.PRIVATE =>
            resourceUserRepository.findByResourceAndUser(resource.id, userId).flatMap {
              case Some(ru) if !ru.deleted && ru.resourceUserType == ResourceUserType.OWNER =>
                // Es el propietario → Acceso completo
                Future.successful(())

              case Some(ru)
                  if !ru.deleted &&
                    (ru.resourceUserType == ResourceUserType.PURCHASED ||
                      ru.resourceUserType == ResourceUserType.ACCEPTED_AS_PAYMENT) =>
                // Lo compró o recibió como pago → Acceso completo
                Future.successful(())

              case _ =>
                // NO tiene acceso → Denegar
                Future.failed(
                  new ForbiddenException("You don't have access to this resource")
                )
            }
        }

      case None =>
        Future.failed(new ResourceNotFoundException(s"Resource ${resourceId} not found"))
    }
  }

  def getAccessLevel(resourceId: ResourceId, userId: UserId): Future[ResourceAccessLevel] = {
    resourceRepository.findById(resourceId).flatMap {
      case Some(resource) =>
        resourceUserRepository.findByResourceAndUser(resource.id, userId).map {
          case Some(ru) if !ru.deleted && ru.resourceUserType == ResourceUserType.OWNER =>
            ResourceAccessLevel.OWNER

          case Some(ru)
              if !ru.deleted &&
                (ru.resourceUserType == ResourceUserType.PURCHASED ||
                  ru.resourceUserType == ResourceUserType.ACCEPTED_AS_PAYMENT ||
                  ru.resourceUserType == ResourceUserType.BARTERED) =>
            ResourceAccessLevel.FULL_ACCESS

          case _ =>
            resource.visibility match {
              case Visibility.PUBLIC =>
                ResourceAccessLevel.FULL_ACCESS

              case Visibility.PRIVATE =>
                ResourceAccessLevel.PREVIEW_ONLY
            }
        }

      case None =>
        Future.failed(new ResourceNotFoundException(s"Resource ${resourceId} not found"))
    }
  }

  override def validateAlreadyHaveAccess(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    resourceUserRepository.findByResourceAndUser(resourceId, userId).flatMap {
      case Some(ru) if !ru.deleted =>
        Future.failed(new ResourceNotAllowedException("You already have access to this resource"))
      case _ =>
        Future.successful(())
    }
  }
}
