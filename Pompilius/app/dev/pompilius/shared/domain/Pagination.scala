package dev.pompilius.shared.domain

case class Pagination(
    offset: Int,
    limit: Option[Int]
) {
  def OneMore: Pagination=this.copy(limit=this.limit.map(_+1))
  def key:String=s"$offset,${limit.map(_.toString).getOrElse("all")}"

}

object Pagination {
  val all: Pagination=Pagination(offset=0,limit=None)
  val single: Pagination=Pagination(offset=0,limit=Some(1))
}
