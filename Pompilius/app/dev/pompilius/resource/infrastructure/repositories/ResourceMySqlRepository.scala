package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.resource.domain._
import dev.pompilius.shared.domain.{Clock, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ResourceMySqlRepository @Inject() (clock: Clock)(
)(implicit dbExecutionContext: DbExecutionContext)
    extends ResourceRepository
    with SQLSyntaxSupport[Resource] {

  override val tableName = "resource"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(r: SyntaxProvider[Resource])(rs: WrappedResultSet): Resource =
    apply(r.resultName)(rs)

  def apply(r: ResultName[Resource])(rs: WrappedResultSet): Resource =
    Resource(
      id = ResourceId(rs.get[Long](r.id)),
      name= rs.get[String](r.name),
      resourceType = ResourceType.withNameInsensitive(rs.get[String](r.resourceType)),
      visibility = Visibility.withNameInsensitive(rs.get[String](r.visibility)),
      created = rs.get(r.created),
      updated = rs.get(r.updated),
      location = rs.get[String](r.location),
      observations = rs.get[Option[String]](r.observations),
      summary = rs.get[Option[String]](r.summary),
      price = rs.get[Option[BigDecimal]](r.price),
      isBarter = rs.get[Boolean](r.isBarter)
    )

  private val r = this.syntax("r")

  override def save(resource: Resource): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> resource.id.id,
          column.name-> resource.name,
          column.resourceType -> resource.resourceType.toString,
          column.visibility -> resource.visibility.toString,
          column.created -> resource.created,
          column.updated -> clock.now,
          column.location -> resource.location,
          column.observations -> resource.observations,
          column.summary -> resource.summary,
          column.price -> resource.price,
          column.isBarter -> resource.isBarter
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

  override def findById(id: ResourceId): Future[Option[Resource]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r).where.eq(r.id, id.id)
        }.map(apply(r.resultName)(_)).single()
      }
    }

  private def filterToSqlSyntax(filter: ResourceFilter): Option[SQLSyntax] = {
    val typeFilter = filter.resourceType.map { rt =>
      sqls.eq(r.resourceType, rt.value)
    }

    val nameFilter= filter.name.map { n =>
      sqls.like(sqls.lower(r.name), ScalikeUtil.normalizeSearch(n.toLowerCase))
    }

    val visibilityFilter = filter.visibility.map { v =>
      sqls.eq(r.visibility, v.value)
    }

    val createdFromFilter = filter.createdFrom.map { cf =>
      sqls.ge(r.created, cf)
    }

    val createdToFilter = filter.createdTo.map { ct =>
      sqls.le(r.created, ct)
    }

    val locationFilter = filter.location.map { loc =>
      sqls.like(sqls.lower(r.location), ScalikeUtil.normalizeSearch(loc.toLowerCase))
    }

    val filters = List(
      typeFilter,
      nameFilter,
      visibilityFilter,
      createdFromFilter,
      createdToFilter,
      locationFilter
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def find(filter: ResourceFilter, pagination: Pagination): Future[List[Resource]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(r.created.desc)
            .append(
              ScalikeUtil.pag(pagination)
            )
        }.map(apply(r.resultName)(_)).list()
      }
    }

  override def delete(id: ResourceId): Future[Done] =
    Future {
      // La eliminación se realiza marcando el ResourceUser como deleted
      // No hacemos nada aquí - el recurso no se elimina directamente
      Done
    }
}
