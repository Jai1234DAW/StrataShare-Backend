package dev.pompilius.shared.infrastructure.binders

import play.api.mvc.QueryStringBindable
import dev.pompilius.Strings
import dev.pompilius.shared.domain.Pagination

object PaginationBinder {

  val defaultLimit: Int = 20

  implicit def paginationBinder(implicit
      intBinder: QueryStringBindable[Int],
      seqStringBinder: QueryStringBindable[Seq[String]]
  ): QueryStringBindable[Pagination] =
    new QueryStringBindable[Pagination] {

      override def bind(
          key: String,
          params: Map[String, Seq[String]]
      ): Option[Either[String, Pagination]] = {

        val offset = intBinder.bind(Strings.offset, params) match {
          case Some(Right(value)) =>
            value
          case _ =>
            0
        }

        val limit = intBinder.bind(Strings.limit, params) match {
          case Some(Right(value)) =>
            Some(value)
          case _ =>
            Some(defaultLimit)
        }

        val orderBy = seqStringBinder.bind(Strings.orderBy, params) match {
          case Some(Right(value)) =>
            value.flatMap(_.trim.split(',').toSeq.map(_.trim)).filter(_.nonEmpty)
          case _ =>
            Seq.empty[String]
        }
        Some(Right(Pagination(offset, limit, orderBy)))

      }

      // Para convertir un objeto Pagination en query string.
      override def unbind(
          key: String,
          pagination: Pagination
      ): String = {
        Seq(
          pagination.limit.map(limit => intBinder.unbind(Strings.limit, limit)),
          Some(intBinder.unbind(Strings.offset, pagination.offset)),
          if (pagination.orderBy.nonEmpty) {
            Some(seqStringBinder.unbind(Strings.orderBy, pagination.orderBy))
          } else None
        ).flatten.mkString("&")
      }

    }

}
