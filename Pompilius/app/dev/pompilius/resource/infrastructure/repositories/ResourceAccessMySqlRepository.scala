package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.resource.domain.{ResourceAccess, ResourceAccessRepository, ResourceId}
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ResourceAccessMySqlRepository @Inject() ()(implicit dbExecutionContext: DbExecutionContext)
    extends ResourceAccessRepository
    with SQLSyntaxSupport[ResourceAccess] {

  override val tableName = "resource_access"

  def apply(ra: SyntaxProvider[ResourceAccess])(rs: WrappedResultSet): ResourceAccess =
    apply(ra.resultName)(rs)

  def apply(ra: ResultName[ResourceAccess])(rs: WrappedResultSet): ResourceAccess =
    ResourceAccess(
      resourceId = ResourceId(rs.get[Long](ra.resourceId)),
      userId = UserId(rs.get[Long](ra.userId))
    )

  private val ra = this.syntax("ra")

  /**
   * Encuentra un acceso a un recurso por ID de recurso e ID de usuario
   */
  override def findByResourceIdAndUserId(resourceId: ResourceId, userId: UserId): Future[Option[ResourceAccess]] =
    Future {
      DB.readOnly { implicit session =>
        withSQL {
          select
            .from(ResourceAccessMySqlRepository as ra)
            .where
            .eq(ra.resourceId, resourceId.id)
            .and
            .eq(ra.userId, userId.id)
        }.map(apply(ra.resultName)).single().apply()
      }
    }

  /**
   * Guarda un acceso a un recurso (usuario puede descargar el recurso)
   */
  override def save(resourceAccess: ResourceAccess): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          insert
            .into(ResourceAccessMySqlRepository)
            .columns(column.resourceId, column.userId)
            .values(resourceAccess.resourceId.id, resourceAccess.userId.id)
        }.execute().apply()
      }
      Done
    }

  /**
   * Elimina el acceso a un recurso para un usuario
   */
  override def delete(resourceId: ResourceId, userId: UserId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(ResourceAccessMySqlRepository)
            .where
            .eq(ra.resourceId, resourceId.id)
            .and
            .eq(ra.userId, userId.id)
        }.execute().apply()
      }
      Done
    }

  /**
   * Obtiene todos los usuarios que tienen acceso a un recurso
   */
  override def findByResourceId(resourceId: ResourceId): Future[List[ResourceAccess]] =
    Future {
      DB.readOnly { implicit session =>
        withSQL {
          select
            .from(ResourceAccessMySqlRepository as ra)
            .where
            .eq(ra.resourceId, resourceId.id)
        }.map(apply(ra.resultName)).list().apply()
      }
    }

  /**
   * Obtiene todos los recursos a los que un usuario tiene acceso
   */
  override def findByUserId(userId: UserId): Future[List[ResourceAccess]] =
    Future {
      DB.readOnly { implicit session =>
        withSQL {
          select
            .from(ResourceAccessMySqlRepository as ra)
            .where
            .eq(ra.userId, userId.id)
        }.map(apply(ra.resultName)).list().apply()
      }
    }
}

