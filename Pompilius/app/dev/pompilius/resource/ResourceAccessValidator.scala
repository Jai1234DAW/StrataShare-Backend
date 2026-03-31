package dev.pompilius.resource.domain

import dev.pompilius.shared.domain.{Pagination, Visibility}
import dev.pompilius.users.domain.UserId

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Validador de acceso a recursos
 * Centraliza la lógica de verificación de permisos
 */
@Singleton
class ResourceAccessValidator @Inject() (
    userResourceRepository: ResourceUserRepository
)(implicit ec: ExecutionContext) {

  /**
   * Verifica que el usuario sea propietario del recurso
   * @param resourceId ID del recurso
   * @param userId ID del usuario
   * @throws Exception si no tiene permisos
   */
  def verifyOwnership(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    userResourceRepository.findByUserAndType(userId, ResourceUserType.OWNER, Pagination.all).map { resourceIds =>
      if (resourceIds.contains(resourceId)) ()
      else throw new Exception("You don't have permission to update this resource")
    }
  }

  /**
   * Valida el acceso a un recurso según su visibilidad
   * - PUBLIC: Acceso para todos
   * - PRIVATE: Solo propietario
   * @param resource El recurso a validar
   * @param userId ID del usuario que intenta acceder
   * @throws Exception si no tiene permisos
   */
  def validateAccess(resource: Resource, userId: UserId): Future[Unit] = {
    resource.visibility match {
      case Visibility.PUBLIC =>
        Future.successful(())

      case Visibility.PRIVATE =>
        verifyOwnership(resource.id, userId).flatMap { _ =>
          isResourceActive(resource.id, userId).map {
            case true => ()
            case false => throw new Exception("You don't have access to this resource")
          }
        }

      case Visibility.SHARED =>
        verifyOwnership(resource.id, userId).recoverWith { _ =>
          userResourceRepository.findByUserAndType(userId, ResourceUserType.PURCHASED, Pagination.all).map { resourceIds =>
            if (resourceIds.contains(resource.id)) ()
            else throw new Exception("You don't have access to this shared resource")
          }
        }
    }
  }

  /**
   * Verifica que el ResourceUser no esté marcado como deleted
   * @param resourceId ID del recurso
   * @param userId ID del usuario
   */
  def isResourceActive(resourceId: ResourceId, userId: UserId): Future[Boolean] = {
    userResourceRepository.findByResourceAndUser(resourceId, userId).map {
      case Some(resourceUser) => !resourceUser.deleted
      case None => false
    }
  }
}

