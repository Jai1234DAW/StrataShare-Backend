package dev.pompilius.transaction.infrastructure.controllers

class TransactionController {

}
//  //Para obtener todas las transacciones completadas del usuario en esto caso pagas
//  def getAllCompletedTransactions(pag: Pagination): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAuthenticatedUser {
//        case (_, user, _) =>
//
//          val filter = TransactionFilter(
//            buyerId = Some(user.id),
//            transactionType = Some(TransactionType.PAYMENT),
//            transactionStatus = Some(TransactionStatus.COMPLETED)
//          )
//
//          for {
//            transactions <- transactionRepository.find(filter, pag.oneMore)
//            transactionsJson<- transactionWriter.toJsonList(transactions)
//
//          } yield Ok(
//            Json.obj(
//
//          )
//      }
//    }
//

//  def getTransactionsByFilter(
//      buyerId: Option[String] = None,
//      sellerId: Option[String] = None,
//      resourceId: Option[String] = None,
//      transactionType: Option[String] = None,
//      transactionStatus: Option[String] = None,
//      page: Int = 1,
//      limit: Int = 20
//  ): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAuthenticatedUser {
//        case (_, user, _) =>
//
//
//          val filter = TransactionFilter(
//            buyerId = buyerId.map(UserId(_)).orElse(Some(user.id)),
//            sellerId = sellerId.map(UserId(_)),
//            resourceId = resourceId.map(ResourceId(_)),
//            transactionType = transactionType.map(TransactionType.withName),
//            transactionStatus = transactionStatus.map(TransactionStatus.withName)
//          )
//
//          val pagination = Pagination(page = page, limit = Some(limit))
//
//          for {
//            transactions <- transactionRepository.find(filter, pagination)
//          } yield Ok(
//            Json.obj(
//              "page" -> page,
//              "limit" -> limit,
//              "filters" -> Json.obj(
//                "buyerId" -> filter.buyerId.map(_.toString),
//                "sellerId" -> filter.sellerId.map(_.toString),
//                "resourceId" -> filter.resourceId.map(_.toString),
//                "transactionType" -> filter.transactionType.map(_.toString),
//                "transactionStatus" -> filter.transactionStatus.map(_.toString)
//              ),
//              "data" -> Json.toJson(transactions)
//            )
//          )
//      }
//    }

//  def getTransactionById(transactionId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAuthenticatedUser {
//        case (_, user, _) =>
//
//
//          for {
//            transaction <-
//              transactionRepository
//                .findById(TransactionId(transactionId))
//                .map(_.getOrElse(throw new Exception("Transaction not found")))
//
//            // Verificar que el usuario es el comprador o vendedor
//            _ = if (transaction.buyerId != user.id && transaction.sellerId != user.id) {
//              throw new Exception("Not authorized to view this transaction")
//            }
//
//          } yield Ok(Json.toJson(transaction))
//      }
//    }

//Esto va en el otro modulo, aquí no porque estaría mezclando responsabilidades, aquí solo se obtienen
//
//  def getMyTransactions(
//      role: Option[String] = None, // "buyer" o "seller"
//      status: Option[String] = None,
//      page: Int = 1,
//      limit: Int = 20
//  ): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAuthenticatedUser {
//        case (_, user, _) =>
//
//
//          val filter = role match {
//            case Some("buyer") =>
//              TransactionFilter(
//                buyerId = Some(user.id),
//                transactionType = Some(TransactionType.PAYMENT),
//                transactionStatus = status.map(TransactionStatus.withName)
//              )
//            case Some("seller") =>
//              TransactionFilter(
//                sellerId = Some(user.id),
//                transactionType = Some(TransactionType.PAYMENT),
//                transactionStatus = status.map(TransactionStatus.withName)
//              )
//            case _ =>
//              TransactionFilter(
//                buyerId = Some(user.id),
//                transactionType = Some(TransactionType.PAYMENT),
//                transactionStatus = status.map(TransactionStatus.withName)
//              )
//          }
//
//          val pagination = Pagination(page = page, limit = Some(limit))
//
//          for {
//            transactions <- transactionRepository.find(filter, pagination)
//          } yield Ok(
//            Json.obj(
//              "page" -> page,
//              "limit" -> limit,
//              "role" -> role,
//              "status" -> status,
//              "data" -> Json.toJson(transactions)
//            )
//          )
//      }
//    }