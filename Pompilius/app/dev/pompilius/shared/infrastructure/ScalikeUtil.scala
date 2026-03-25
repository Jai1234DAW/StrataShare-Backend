package dev.pompilius.shared.infrastructure

import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax
import dev.pompilius.shared.domain.Pagination

object ScalikeUtil {

  def normalizeSearch(s: String): String = {
    ("%" + s.toLowerCase + "%").replaceAll("( |%)+", "%")
  }

  def onDuplicateUpdate(tuples: (SQLSyntax, ParameterBinder)*): SQLSyntax = {
    sqls" ON DUPLICATE KEY UPDATE ${sqls.csv(
      tuples.map(each => sqls"${each._1} = ${each._2}"): _*
    )} "
  }

  def onDuplicateUpdate(key: SQLSyntax, tuples: (SQLSyntax, ParameterBinder)*): SQLSyntax = {
    sqls" ON DUPLICATE KEY UPDATE ${sqls.csv(
      tuples.map(each => sqls"${each._1} = if ($key = values($key), ${each._2}, ${each._1})"): _*
    )} "
  }

  def onDuplicateUpdate(key1: SQLSyntax, key2: SQLSyntax, tuples: (SQLSyntax, ParameterBinder)*): SQLSyntax = {
    sqls" ON DUPLICATE KEY UPDATE ${sqls.csv(
      tuples.map(each => sqls"${each._1} = if ($key1 = values($key1) and $key2 = values($key2), ${each._2}, ${each._1})"): _*
    )} "
  }

  def onDuplicateIncrement(tuples: (SQLSyntax, ParameterBinder)*): SQLSyntax = {
    sqls" ON DUPLICATE KEY UPDATE ${sqls.csv(
      tuples.map(each => sqls"${each._1} = ${each._1} + ${each._2}"): _*
    )} "
  }

  def coalesce[A](column: SQLSyntax, value: A)(implicit evA: ParameterBinderFactory[A]): SQLSyntax = {
    sqls" coalesce($column, ${evA(value)}) "
  }

  def date(column: SQLSyntax): SQLSyntax = sqls" date($column) "

  def year(column: SQLSyntax): SQLSyntax = sqls" year($column) "

  def month(column: SQLSyntax): SQLSyntax = sqls" month($column) "

  def day(column: SQLSyntax): SQLSyntax = sqls" day($column) "

  def dayOfWeek(column: SQLSyntax): SQLSyntax = sqls" dayOfWeek($column) "

  def hour(column: SQLSyntax): SQLSyntax = sqls" hour($column) "

  def greatest[A](column: SQLSyntax, value: A)(implicit
                                               ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" greatest($column, ${ev(value)}) "

  def addMonth[A](column: SQLSyntax, value: A)(implicit
                                               ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" date_add($column, interval ${ev(value)} month) "

  def addDay[A](column: SQLSyntax, value: A)(implicit
                                             ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" date_add($column, interval ${ev(value)} day) "

  def div[A](column: SQLSyntax, value: A)(implicit
                                          ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" $column / ${ev(value)} "

  def mul[A](column: SQLSyntax, value: A)(implicit
                                          ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" $column * ${ev(value)} "

  def plus[A](column: SQLSyntax, value: A)(implicit
                                           ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" $column + ${ev(value)} "

  def minus[A](column: SQLSyntax, value: A)(implicit
                                            ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" $column - ${ev(value)} "

  def jsonContains[A](column: SQLSyntax, value: String): SQLSyntax = {
    sqls" json_contains($column, $value) "
  }

  def when[A, B, C, D, E](
                           column: SQLSyntax,
                           option1: A,
                           value1: B,
                           option2: C,
                           value2: D,
                           default: E
                         )(implicit
                           evA: ParameterBinderFactory[A],
                           evB: ParameterBinderFactory[B],
                           evC: ParameterBinderFactory[C],
                           evD: ParameterBinderFactory[D],
                           evE: ParameterBinderFactory[E]
                         ): SQLSyntax =
    sqls" case $column when ${evA(option1)} then ${evB(value1)} when ${evC(
      option2
    )} then ${evD(value2)}  else ${evE(default)} end "

  def ?[A, B](column: SQLSyntax, value1: A, value2: B)(implicit
                                                       evA: ParameterBinderFactory[A],
                                                       evB: ParameterBinderFactory[B]
  ): SQLSyntax = {
    sqls" if($column, ${evA(value1)}, ${evB(value2)}) "
  }

  def andOrWhere(syntax: SQLSyntax, part: SQLSyntax): SQLSyntax =
    if (syntax.isEmpty) sqls.where(part) else syntax.and(part)

  def safeEq[A](column: SQLSyntax, value: A)(implicit
                                             ev: ParameterBinderFactory[A]
  ): SQLSyntax = sqls" $column <=> ${ev(value)} "

  def pag(p: Pagination)(implicit ev: ParameterBinderFactory[Int]): SQLSyntax = {
    p.limit match {
      case Some(limit) =>
        sqls" limit ${ev(limit)} offset ${ev(p.offset)} "
      case _ =>
        sqls.empty
    }
  }
}
