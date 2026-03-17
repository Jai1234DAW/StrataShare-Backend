package dev.pompilius.users.infrastructure.repositories

import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.{Role, UserId, UserRole, UserRoleFilter, UserRoleRepository}
import org.apache.pekko.Done
import dev.pompilius.shared.infrastructure.ScalikeUtil
import scalikejdbc._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UserRoleMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext)
    extends UserRoleRepository
    with SQLSyntaxSupport[UserRole] {

  //Usabilidad de esto
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))
  override val tableName = "users_role"

  def apply(ur: SyntaxProvider[UserRole])(rs: WrappedResultSet): UserRole =
    apply(ur.resultName)(rs)

  def apply(ur: ResultName[UserRole])(rs: WrappedResultSet): UserRole =
    UserRole(
      userId = UserId(rs.get[Long](ur.userId)),
      role = Role.withNameInsensitive(rs.get[String](ur.role))
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

  //Mirar estos métodos
  override def findBy(userId: UserId, role: Role): Future[Option[UserRole]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ur).where.eq(ur.userId, userId.id).and.eq(ur.role, role.toString)
        }.map(apply(ur.resultName)(_)).single()
      }
    }

  //MIRAR ESTO
  private def filterToSqlSyntax(filter: UserRoleFilter): Option[SQLSyntax] = {
    val filters = List(
      filter.userId.map(id => sqls.eq(ur.userId, id.id)),
      filter.role.map(role => sqls.eq(ur.role, role.toString))
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def find(filter: UserRoleFilter, pag: Pagination): Future[List[UserRole]] =
    Future {

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ur)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(ur.userId, ur.role)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(ur.resultName)(_)).list()
      }
    }

  override def save(userRole: UserRole): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.userId -> userRole.userId.id,
          column.role -> userRole.role.toString
        )

        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(values: _*))
        }.update()
      }
      Done
    }

  override def setUserRoles(userId: UserId, roles: Set[Role]): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.userId, userId.id)
        }.update()

        val params: List[Seq[Any]] = roles.toList.map(role => Seq(userId.id, role.toString))

        if (params.nonEmpty) {
          withSQL {
            insert
              .into(this)
              .namedValues(
                column.userId -> sqls.?,
                column.role -> sqls.?
              )
          }.batch(params: _*).apply()
        }
      }
      Done
    }

}
