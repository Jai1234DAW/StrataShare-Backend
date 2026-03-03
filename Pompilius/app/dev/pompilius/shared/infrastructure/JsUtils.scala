package dev.pompilius.shared.infrastructure

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

object JsUtils {

  def toJsValueWrapper[T](field: (String, T))(implicit w: Writes[T]): Option[(String, JsValueWrapper)] = {
    field match {
      case (_, None) =>
        None
      case _ =>
        Some(field._1, Json.toJsFieldJsValueWrapper(field._2))
    }
  }
}

//MIRAR SI LUEGO NECESITO LOS OTROS MÉTODOS
