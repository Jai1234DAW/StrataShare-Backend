package dev.pompilius.user.infrastructure.repositories

import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.user.domain.{RoleId, UserId, UserRole, UserRoleRepository}
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UserRoleMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext)
    extends UserRoleRepository
    with SQLSyntaxSupport[UserRole] {

  override val tableName = "user_role"

  def apply(ur: SyntaxProvider[UserRole])(rs: WrappedResultSet): UserRole =
    apply(ur.resultName)(rs)

  def apply(ur: ResultName[UserRole])(rs: WrappedResultSet): UserRole =
    UserRole(
      userId = UserId(rs.get[Long](ur.userId)),
      roleId = RoleId(rs.get[Long](ur.roleId))
    )

  val ur: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[UserRole], UserRole] = {
    this.syntax("ur")
  }

  override def getAllByUserId(userId: UserId): Future[List[UserRole]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ur).where.eq(ur.userId, userId.id)
        }.map(apply(ur.resultName)(_)).list()
      }
    }

  override def setUserRoles(userId: UserId, roles: List[RoleId]): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.userId, userId.id)
        }.update()

        val params: List[Seq[Any]] = roles.map(roleId => Seq(userId.id, roleId.id))
        withSQL {
          insert
            .into(this)
            .namedValues(
              column.userId -> sqls.?,
              column.roleId -> sqls.?
            )
        }.batch(params: _*).apply()
      }
      Done
    }
}
