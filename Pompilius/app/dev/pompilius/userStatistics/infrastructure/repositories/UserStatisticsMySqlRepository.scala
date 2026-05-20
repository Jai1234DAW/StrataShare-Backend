package dev.pompilius.userStatistics.infrastructure.repositories

import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.userStatistics.domain.{UserStatistics, UserStatisticsId}
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UserStatisticsMySqlRepository @Inject() (implicit ec: DbExecutionContext)
  extends SQLSyntaxSupport[UserStatistics] {

  override val tableName = "user_statistics"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(us: SyntaxProvider[UserStatistics])(rs: WrappedResultSet): UserStatistics =
    apply(us.resultName)(rs)

  def apply(s: ResultName[UserStatistics])(rs: WrappedResultSet): UserStatistics =
    UserStatistics(
      id=UserStatisticsId(rs.get[Long](us.id)),
      userId = UserId(rs.get[Long](s.userId)),
      totalFollowers = rs.get(us.totalFollowers),
      totalPurchases = rs.get(us.totalPurchases),
      totalSales = rs.get(us.totalSales),
      totalBarters = rs.get(us.totalBarters),
      totalOffers = rs.get(us.totalOffers),
      mostSoldResource = rs.getOpt(s.mostSoldResource),
      mostBarteredResource = rs.getOpt(s.mostBarteredResource),
      mostOfferedResource = rs.getOpt(s.mostOfferedResource),
      totalResourcesUploaded=rs.get(us.totalResourcesUploaded),
      dateRange=rs.get(s.dateRange)
    )

  private val us = this.syntax("us")

  // Obtener estadísticas de usuario
  def findByUserId(userId: UserId): Future[Option[UserStatistics]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as us).where.eq(us.userId, userId.id)
        }.map(apply(us.resultName)(_)).single()
      }
    }

  // Guardar/Actualizar estadísticas
  def save(stats: UserStatistics): Future[Done] =
    Future {
      DB.localTx { implicit dbSession =>
        val values = List(
          column.id-> stats.id.id,
          column.userId -> stats.userId.id,
          column.totalFollowers -> stats.totalFollowers,
          column.totalPurchases -> stats.totalPurchases,
          column.totalSales -> stats.totalSales,
          column.totalBarters -> stats.totalBarters,
          column.totalOffers -> stats.totalOffers,
          column.mostSoldResource -> stats.mostSoldResource,
          column.mostBarteredResource -> stats.mostBarteredResource,
          column.mostOfferedResource -> stats.mostOfferedResource,
          column.totalResourcesUploaded -> stats.totalResourcesUploaded,
          column.dateRange -> stats.dateRange
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
}

