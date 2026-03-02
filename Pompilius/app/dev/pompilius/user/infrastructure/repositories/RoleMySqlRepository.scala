package dev.pompilius.user.infrastructure.repositories

import dev.pompilius.shared.domain.Pagination
import scalikejdbc.SQLSyntaxSupport

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import dev.pompilius.user.domain.{Role, RoleId, RoleRepository}



@Singleton

@Singleton
class RoleMySqlRepository @Inject() (rolePermissionMySqlRepository: RolePermissionMySqlRepository)(implicit
                                                                                                   dbExecutionContext: DbExecutionContext
) extends SQLSyntaxSupport[Role]
  with RoleRepository {

  override val tableName = "roles"

  def apply(r: SyntaxProvider[Role])(rs: WrappedResultSet): Role =
    apply(r.resultName)(rs)
  def apply(r: ResultName[Role])(rs: WrappedResultSet): Role =
    Role(
      id = RoleId(rs.get[Long](r.id)),
      name = rs.get(r.name),
      notes = rs.get(r.notes)
    )

  val r: scalikejdbc.QuerySQLSyntaxProvider[scalikejdbc.SQLSyntaxSupport[Role], Role] = {
    this.syntax("r")
  }

  def findById(roleId: RoleId): Future[Option[Role]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r).where.eq(r.id, roleId.id)
        }.map(apply(r.resultName)(_)).single()
      }
    }

  def findByName(name: String): Future[Option[Role]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r).where.eq(r.name, name)
        }.map(apply(r.resultName)(_)).single()
      }
    }

  def find(name: Option[String], pag: Pagination): Future[List[Role]] =
    Future {
      DB.localTx { implicit session =>
        val normalizedName = name.map(s => ("%" + s + "%").replaceAll("( |%)+", "%"))

        withSQL {
          selectFrom(this as r)
            .where(normalizedName.map(s => sqls.like(r.name, s)))
            .orderBy(r.id)
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(r.resultName)(_)).list()
      }
    }

  def getAll: Future[List[Role]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r)
        }.map(apply(r.resultName)(_)).list()
      }
    }

  def save(role: Role): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> role.id.id,
          column.name -> role.name,
          column.notes -> role.notes
        )

        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.id, values: _*))
        }.update()
      }
      Done
    }

  override def delete(roleId: RoleId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        // Borramos los permisos asociados al rol
        withSQL {
          deleteFrom(rolePermissionMySqlRepository).where.eq(rolePermissionMySqlRepository.column.roleId, roleId.id)
        }.update()

        // Borramos el rol
        withSQL {
          deleteFrom(this).where.eq(column.id, roleId.id)
        }.update()
      }
      Done
    }

}