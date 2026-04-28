package dev.pompilius.review.infrastructure.repositories

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.review.domain.{Review, ReviewId, ReviewRepository}
import dev.pompilius.shared.domain.{Clock, Pagination}
import dev.pompilius.users.domain.UserId
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
class ReviewMySqlRepository @Inject()(implicit ec: DbExecutionContext)
    extends ReviewRepository
    with SQLSyntaxSupport[Review] {

  override val tableName = "review"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(r: SyntaxProvider[Review])(rs: WrappedResultSet): Review = apply(r.resultName)(rs)

  def apply(r: ResultName[Review])(rs: WrappedResultSet): Review =
    Review(
      id = ReviewId(rs.get[Long](r.id)),
      userId = UserId(rs.get[Long](r.userId)),
      resourceId = ResourceId(rs.get[Long](r.resourceId)),
      rating = rs.get(r.rating),
      comment = rs.get(r.comment),
      createdAt = rs.get(r.createdAt),
      updatedAt = rs.get(r.updatedAt)
    )

  private val r = this.syntax("r")

  override def save(review: Review): Future[Done] = {
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> review.id.id,
          column.userId -> review.userId.id,
          column.resourceId -> review.resourceId.id,
          column.rating -> review.rating,
          column.comment -> review.comment,
          column.createdAt -> review.createdAt,
          column.updatedAt -> review.updatedAt
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
  }

  override def findById(id: ReviewId): Future[Option[Review]] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r).where.eq(r.id, id.id)
        }.map(apply(r.resultName)(_)).single()
      }
    }
  }

  override def findByResource(resourceId: ResourceId, pag: Pagination): Future[List[Review]] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r).where
            .eq(r.resourceId, resourceId.id)
            .orderBy(r.createdAt)
            .desc
            .append(ScalikeUtil.pag(pag))
        }.map(apply(r.resultName)(_)).list()
      }
    }
  }

  override def findByResourceAndUser(resourceId: ResourceId, userId: UserId): Future[Option[Review]] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as r).where
            .eq(r.resourceId, resourceId.id)
            .and
            .eq(r.userId, userId.id)
        }.map(apply(r.resultName)(_)).single()
      }
    }
  }

  override def getAverageRating(resourceId: ResourceId): Future[Double] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          select(sqls.avg(r.rating)).from(this as r).where.eq(r.resourceId, resourceId.id)
        }.map(_.doubleOpt(1)).single().flatten.getOrElse(0.0)
      }
    }
  }

  override def getReviewCount(resourceId: ResourceId): Future[Int] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          select(sqls.count).from(this as r).where.eq(r.resourceId, resourceId.id)
        }.map(_.int(1)).single().getOrElse(0)
      }
    }
  }

  override def getCommentsCount(resourceId: ResourceId): Future[Int] = {
    Future{
        DB.localTx { implicit session =>
            withSQL {
            select(sqls.count).from(this as r).where
                .eq(r.resourceId, resourceId.id)
                .and
                .isNotNull(r.comment)
            }.map(_.int(1)).single().getOrElse(0)
        }
    }
  }

  override def delete(id: ReviewId): Future[Done] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.id, id.id)
        }.update()
      }
      Done
    }
  }
}
