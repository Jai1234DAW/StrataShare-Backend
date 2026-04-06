package dev.pompilius.resource.infrastructure

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.exceptions.{ResourceNotAllowedException, ResourceNotFoundException}
import dev.pompilius.resource.domain.{ResourceId, ResourceRepository, ResourceUserRepository, ResourceUserType}
import dev.pompilius.shared.domain.{Pagination, Visibility}
import dev.pompilius.shared.domain.exceptions.ForbiddenException
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.users.domain.UserId

import javax.inject.{Inject,Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ResourceAccessValidatorImpl])
trait ResourceAccessValidator {

  def verifyOwnership(resourceId: ResourceId, userId: UserId): Future[Unit]
  def validateAccess(resourceId: ResourceId, userId: UserId): Future[Unit]

}

@Singleton
class ResourceAccessValidatorImpl @Inject() (
  resourceUserRepository: ResourceUserRepository,
  resourceRepository: ResourceRepository)(implicit ec: ExecutionContext)
 extends ResourceAccessValidator {


  def verifyOwnership(resourceId: ResourceId, userId: UserId)
                   : Future[Unit] = {

    resourceUserRepository
      .findByUserAndType(userId, ResourceUserType.OWNER, Pagination.all)
      .flatMap { resourceIds =>
        if (resourceIds.contains(resourceId)) {
          Future.successful(())
        } else {
          isResourceActive(resourceId, userId).flatMap {
            case true => Future.successful(())
            case false => Future.failed(
              new ResourceNotAllowedException(
                "You don't have permission to get or update this resource"
              )
            )
          }
        }
      }
  }


  private def isResourceActive(resourceId: ResourceId, userId: UserId): Future[Boolean] = {
    resourceUserRepository.findByResourceAndUser(resourceId, userId).map {
      case Some(resourceUser) => !resourceUser.deleted
      case None => false
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

}
