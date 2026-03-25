package dev.pompilius.shared.domain

case class Pagination(
    offset: Int,
    limit: Option[Int],
    orderBy: Seq[String] = Seq.empty
) {
  def oneMore: Pagination = this.copy(limit = this.limit.map(_ + 1))
  def key: String = s"$offset,${limit.map(_.toString).getOrElse("all")}"

}

object Pagination {
  def apply(limit: Int): Pagination = Pagination(offset = 0, limit = Some(limit), orderBy = Seq.empty)
  val all: Pagination = Pagination(offset = 0, limit = None, orderBy = Seq.empty)
  val single: Pagination = Pagination(offset = 0, limit = Some(1),  orderBy = Seq.empty)
}
