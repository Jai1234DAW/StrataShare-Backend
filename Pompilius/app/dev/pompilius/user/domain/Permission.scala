package dev.pompilius.user.domain

import enumeratum.{Enum,EnumEntry, PlayJsonEnum}

sealed trait Permission extends EnumEntry

object Permission extends Enum[Permission] with PlayJsonEnum[Permission] {

  val values: IndexedSeq[Permission]=findValues

  //Admin
  @SuppressWarnings(Array("ObjectNames"))
  case object ADMIN_ROLES extends Permission
  @SuppressWarnings(Array("ObjectNames"))
  case object ADMIN_USERS extends Permission
  @SuppressWarnings(Array("ObjectNames"))
  case object ADMIN_REGIONS extends Permission

  // Readonly
  @SuppressWarnings(Array("ObjectNames"))
  case object VIEW_ROLES extends Permission
  @SuppressWarnings(Array("ObjectNames"))
  case object VIEW_USERS extends Permission
  @SuppressWarnings(Array("ObjectNames"))
  case object VIEW_REGIONS extends Permission

  // Support
  @SuppressWarnings(Array("ObjectNames"))
  case object SUPPORT extends Permission
  @SuppressWarnings(Array("ObjectNames"))
  case object LOGIN_AS extends Permission
  @SuppressWarnings(Array("ObjectNames"))
  case object VIEW_ALL_REPORTS extends Permission

  // Users
  // Estudiante
  case object CREATE_STUDIES        extends Permission
  case object EDIT_STUDIES          extends Permission
  case object DELETE_STUDIES        extends Permission
  case object CREATE_SAMPLES        extends Permission
  case object EDIT_SAMPLES          extends Permission
  case object DELETE_SAMPLES        extends Permission
  case object RATE_STUDIES          extends Permission   // valoraciones generales
  case object REQUEST_ACCESS        extends Permission   // archivos de otros usuarios
  case object PARTICIPATE_TRADES    extends Permission   // trueques limitados
  case object MONETIZE_RESOURCES    extends Permission   // venta a través de pasarela
  case object VIEW_MAPS             extends Permission
  case object VIEW_STATS_BASIC      extends Permission
  case object VIEW_HISTORY          extends Permission //historial de los trades o transacciones realizadas

  // Profesional
  case object UNLIMITED_TRADES      extends Permission
  case object VIEW_STATS_FULL       extends Permission
  case object RATE_CREDIBILITY      extends Permission    // valorar otros usuarios
  case object GRADE_STUDIES         extends Permission    // calificar estudios (solo si es profesional)

  // Aficionado
  case object VIEW_PUBLIC_STUDIES   extends Permission
  case object COMMENT_PUBLIC        extends Permission
  case object LIMITED_TRADES        extends Permission    // trueques limitados
  case object PURCHASE_RESOURCES    extends Permission
  case object VIEW_HISTORY_LIMITED  extends Permission
}