package dev.pompilius.shared.domain


case class Paginated[T](items: List[T], moreItems: Boolean, count: Option[Int]=None)

object Paginated {

  def apply[T](items: List[T], pag: Pagination): Paginated[T] = {
    pag.limit match {
      case Some(limit) =>
        Paginated(items = items.take(limit), items.length > limit)
      case _ =>
        Paginated(items = items, moreItems = false)
    }
  }

}
