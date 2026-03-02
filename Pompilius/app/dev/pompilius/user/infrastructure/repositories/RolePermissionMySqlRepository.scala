package dev.pompilius.user.infrastructure.repositories

import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.user.domain.{Permission, RoleId, RolePermission, RolePermissionRepository}
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RolePermissionMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext)
    extends RolePermissionRepository
    with SQLSyntaxSupport[RolePermission] {

  override val tableName = "role_permissions"

  def apply(rp: SyntaxProvider[RolePermission])(
      rs: WrappedResultSet
  ): RolePermission = apply(rp.resultName)(rs)

  def apply(
      rp: ResultName[RolePermission]
  )(rs: WrappedResultSet): RolePermission =
    RolePermission(
      roleId = rs.get(rp.roleId),
      permission = Permission.withNameInsensitive(rs.get(rp.permission))
    )

  val rp: QuerySQLSyntaxProvider[SQLSyntaxSupport[RolePermission], RolePermission] = {
    this.syntax("rp")
  }

  override def getAllByRoleId(roleId: RoleId): Future[List[RolePermission]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as rp).where.eq(rp.roleId, roleId.id)
        }.map(apply(rp.resultName)(_)).list()
      }
    }

  override def setRolePermissions(roleId: RoleId, permissions: List[Permission]): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.roleId, roleId.id)
        }.update()

        val params: List[Seq[Any]] = permissions.map(permission => Seq(roleId.id, permission.toString))
        withSQL {
          insert
            .into(this)
            .namedValues(
              column.roleId -> sqls.?,
              column.permission -> sqls.?
            )
        }.batch(params: _*).apply()
      }
      Done
    }
}
