package dev.pompilius.worker.application

import dev.pompilius.barter.domain.{BarterFilter, BarterRepository}
import dev.pompilius.shared.domain.{Clock, Pagination}
import dev.pompilius.transaction.domain.{TransactionFilter, TransactionRepository, TransactionStatus, TransactionType}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.Done
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Worker que cancela automáticamente transacciones de tipo BARTER
 * que llevan más de 7 días en estado PENDING.
 *
 * Se ejecuta cada 24 horas comenzando 1 hora después del inicio de la aplicación.
 */
@Singleton
class BarterPendingTransactionWorker @Inject() (
    actorSystem: ActorSystem,
    transactionRepository: TransactionRepository,
    barterRepository: BarterRepository,
    clock: Clock
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)
  private val SEVEN_DAYS_IN_MILLIS = 7L * 24L * 60L * 60L * 1000L

  // Iniciar el scheduler al crear la instancia (singleton)
  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = 1.hour,
    interval = 24.hours
  ) { () =>
    cancelExpiredPendingBarterTransactions().recover { case e =>
      logger.error("Error cancelling expired pending barter transactions", e)
    }
  }

  /**
   * Busca todas las transacciones BARTER en estado PENDING que fueron creadas
   * hace más de 7 días y las cambia a CANCELLED.
   *
   * @return Future[Done] cuando se completa la operación
   */
  private def cancelExpiredPendingBarterTransactions(): Future[Done] = {
    val transactionFilter = TransactionFilter(
      transactionType = Some(TransactionType.BARTER),
      transactionStatus = Some(TransactionStatus.PENDING)
    )

    for {
      // Obtener todas las transacciones BARTER pendientes (sin limite de paginación)
      pendingBarterTransactions <- transactionRepository.find(transactionFilter, Pagination(0, None))

      // Filtrar aquellas que fueron creadas hace más de 7 días
      expiredTransactions = pendingBarterTransactions.filter { transaction =>
        val nowMillis = clock.now.getMillis
        val createdMillis = transaction.created.getMillis
        val ageMillis = nowMillis - createdMillis
        ageMillis > SEVEN_DAYS_IN_MILLIS
      }

      // Obtener información de las transacciones expiradas (opcional para logging)
      _ <- if (expiredTransactions.nonEmpty) {
        for {
          barterInfos <- Future.traverse(expiredTransactions) { transaction =>
            barterRepository.findByTransactionId(transaction.id).map { barterOpt =>
              (transaction.id.id, barterOpt.map(_.barterId.id))
            }
          }
        } yield barterInfos
      } else {
        Future.successful(List.empty)
      }

      // Cancelar cada una de las transacciones expiradas
      _ <- Future.traverse(expiredTransactions) { transaction =>
        transactionRepository.updateStatusRejectedCancelled(transaction.id, TransactionStatus.CANCELLED)
      }

      // Log del resultado
      _ = if (expiredTransactions.nonEmpty) {
        logger.info(
          s"[BarterPendingTransactionWorker] Cancelled ${expiredTransactions.length} expired pending barter transaction(s): ${expiredTransactions.map(_.id.id).mkString(", ")}"
        )
      } else {
        logger.debug("[BarterPendingTransactionWorker] No expired pending barter transactions found to cancel")
      }
    } yield Done
  }
}

