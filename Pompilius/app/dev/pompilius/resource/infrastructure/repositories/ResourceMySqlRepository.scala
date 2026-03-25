package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.resource.domain.{Resource, ResourceFilter, ResourceId, ResourceRepository, ResourceType}
import dev.pompilius.resource.domain.sample.{Sample, SampleRepository}
import dev.pompilius.resource.domain.study.{Study, StudyRepository}
import dev.pompilius.shared.domain.{Pagination, Visibility}
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._


@Singleton
class ResourceMySqlRepository @Inject() (
    sampleRepository: SampleRepository,
    studyRepository: StudyRepository
)(implicit dbExecutionContext: DbExecutionContext)
    extends ResourceRepository
    with SQLSyntaxSupport[Resource] {

  override val tableName = "resource"

  def apply(r: SyntaxProvider[Resource])(rs: WrappedResultSet): Resource =
    apply(r.resultName)(rs)

  def apply(r: ResultName[Resource])(rs: WrappedResultSet): Resource =
      Resource(
        id=ResourceId(rs.get[Long](r.id)),
        resourceType = ResourceType.withNameInsensitive(rs.get[String](r.resourceType)),
        ownerId = UserId(rs.get[Long](r.ownerId)),
        visibility = Visibility.withNameInsensitive(rs.get[String](r.visibility)),
        created = rs.get(r.created),
        updated = rs.get(r.updated),
        localization = rs.get(r.localization)
    )

  private val r = this.syntax("r")

  override def save(resource: Resource, sample: Option[Sample], study: Option[Study]): Future[Done] =
    for {
      // Primero guardamos el recurso base
      _ <- saveResourceBase(resource)

      // Luego guardamos los datos específicos según el tipo
      _ <- resource.resourceType match {
        case ResourceType.SAMPLE =>
          sample match {
            case Some(s) => sampleRepository.save(s)
            //Preguntar esto, porque si el tipo es Sample, se espera un Sample, pero si no viene, no se debería guardar nada? O se debería eliminar el recurso base?
            case None => Future.successful(Done)
          }
        case ResourceType.STUDY =>
          study match {
            case Some(s) => studyRepository.save(s)
            case None => Future.successful(Done)
          }
        case _ => Future.successful(Done)
      }
    } yield Done


  private def saveResourceBase(resource: Resource): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> resource.id.id,
          column.resourceType -> resource.resourceType.toString,
          column.ownerId -> resource.ownerId.id,
          column.visibility -> resource.visibility.toString,
          column.created -> resource.created,
          column.updated -> resource.updated,
          column.localization -> resource.localization
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
          selectFrom(this as r)
            .where
            .eq(r.id, id.id)
            .and
            .eq(r.ownerId, ownerId.id)
        }.map(apply(r.resultName)(_)).single()
      }
    }

  private def filterToSqlSyntax(filter: ResourceFilter): Option[SQLSyntax] = {
    val typeFilter = filter.resourceType.map { rt =>
      sqls.eq(r.resourceType, rt.value)
    }

    val ownerFilter = filter.ownerId.map { oid =>
      sqls.eq(r.ownerId, oid.id)
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

    val localizationFilter=filter.localization.map{ loc =>
      sqls.like(sqls.lower(r.localization), s"%${loc.toLowerCase}%")
    }

    val filters = List(
      typeFilter,
      ownerFilter,
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
        }.map(apply(r)).list()
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
}
