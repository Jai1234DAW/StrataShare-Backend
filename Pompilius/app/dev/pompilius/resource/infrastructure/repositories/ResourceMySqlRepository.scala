package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.resource.domain.{Resource, ResourceFilter, ResourceId, ResourceRepository, ResourceType, Sample, Study}
import dev.pompilius.resource.domain.study.Area
import dev.pompilius.shared.domain.{Pagination, Visibility}
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scalikejdbc.jodatime.JodaTypeBinder._

@Singleton
class ResourceMySqlRepository @Inject() (
)(implicit dbExecutionContext: DbExecutionContext)
    extends ResourceRepository
    with SQLSyntaxSupport[Resource] {

  override val tableName = "resource"

  def apply(r: SyntaxProvider[Resource])(rs: WrappedResultSet): Resource =
    apply(r.resultName)(rs)

  def apply(r: ResultName[Resource])(rs: WrappedResultSet): Resource = {
    val id = ResourceId(rs.get[Long](r.id))
    val resourceType = ResourceType.withNameInsensitive(rs.get[String](r.resourceType))
    val deleted = rs.get[Boolean](r.deleted)
    val visibility = Visibility.withNameInsensitive(rs.get[String](r.visibility))
    val created = rs.get(r.created)
    val updated = rs.get(r.updated)
    val localization = rs.get[String](r.localization)
    val observations = rs.get[Option[String]](r.observations)
    val summary = rs.get[Option[String]](r.summary)

    resourceType match {
      case ResourceType.SAMPLE =>
        Sample(
          id = id,
          resourceType = ResourceType.SAMPLE,
          deleted = deleted,
          visibility = visibility,
          created = created,
          updated = updated,
          localization = localization,
          observations = observations,
          summary = summary,
          name = "",
          minerals = None,
          collectionMethods = None,
          isFresh = false,
          sampleType = None,
          materialsUsed = None,
          rockType = None,
          geologicalProcesses = None
        )
      case ResourceType.STUDY =>
        Study(
          id = id,
          resourceType = ResourceType.STUDY,
          deleted = deleted,
          visibility = visibility,
          created = created,
          updated = updated,
          localization = localization,
          observations = observations,
          summary = summary,
          name = "",
          startDate = created,
          endDate = None,
          description = "",
          coordinates = "",
          area = Area.OTHER,
          methods = "",
          authors = "",
          section = false,
          antecedents = true,
          nameSection = None
        )
      case _ =>
        Sample(
          id = id,
          resourceType = ResourceType.SAMPLE,
          deleted = deleted,
          visibility = visibility,
          created = created,
          updated = updated,
          localization = localization,
          observations = observations,
          summary = summary,
          name = "",
          minerals = None,
          collectionMethods = None,
          isFresh = false,
          sampleType = None,
          materialsUsed = None,
          rockType = None,
          geologicalProcesses = None
        )
    }
  }

  private val r = this.syntax("r")

  override def save(resource: Resource): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> resource.id.id,
          column.resourceType -> resource.resourceType.toString,
          column.deleted -> resource.deleted,
          column.visibility -> resource.visibility.toString,
          column.created -> resource.created,
          column.updated -> resource.updated,
          column.localization -> resource.localization,
          column.observations -> resource.observations,
          column.summary -> resource.summary
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

  override def findByIdAndOwner(id: ResourceId, ownerId: UserId): Future[Option[Resource]] =
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

    val visibilityFilter = filter.visibility.map { v =>
      sqls.eq(r.visibility, v.value)
    }

    val createdFromFilter = filter.createdFrom.map { cf =>
      sqls.ge(r.created, cf)
    }

    val createdToFilter = filter.createdTo.map { ct =>
      sqls.le(r.created, ct)
    }

    val localizationFilter = filter.localization.map { loc =>
      sqls.like(sqls.lower(r.localization), s"%${loc.toLowerCase}%")
    }

    val filters = List(
      typeFilter,
      visibilityFilter,
      createdFromFilter,
      createdToFilter,
      localizationFilter
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
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(r.id, id.id)
        }.update()
      }
      Done
    }

  override def findAll(pag: Pagination): Future[List[Resource]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r)
            .orderBy(r.created.desc)
            .append(ScalikeUtil.pag(pag))
        }.map(apply(r.resultName)(_)).list()
      }
    }

  override def findAllByUser(userId: UserId): Future[List[Resource]] =
    Future {
      DB.localTx { implicit session =>
        // Aquí simplemente retorna recursos sin filtrar por usuario
        // El filtrado por usuario lo debe hacer ResourceRepositoryAggregate
        withSQL {
          selectFrom(this as r)
            .orderBy(r.created.desc)
        }.map(apply(r.resultName)(_)).list()
      }
    }
}
