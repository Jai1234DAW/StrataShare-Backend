package dev.pompilius.swagger.infrastructure.controllers

import com.iheart.playSwagger.generator.{NamingConvention, SwaggerSpecGenerator}
import dev.pompilius.shared.infrastructure.JsUtils
import dev.pompilius.users.domain.Role
import play.api.Logger
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject._
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
class SwaggerController @Inject() extends InjectedController {

  private val logger = Logger(this.getClass)

  private val additionalSchemas: JsObject = {
    val source = Source.fromURL(getClass.getResource("/swagger-schemas.json"), "UTF-8")
    val content =
      try {
        source.getLines().mkString("\n")
      } finally {
        source.close()
      }
    Json.parse(content).as[JsObject]
  }

  def getIndexSwagger: Action[AnyContent] =
    Action {
      Ok(dev.pompilius.swagger.infrastructure.views.html.swagger_index()).as(HTML)
    }

  def generateSwaggerSpec: Action[AnyContent] =
    Action {
      SwaggerSpecGenerator(
        NamingConvention.None,
        swaggerV3 = true,
        "Pompilius"
      )(getClass.getClassLoader).generate() match {
        case Success(swaggerSpec) =>
          Ok(
            Json.toJson(
              sortPaths(
                formatTags(
                  includeRoles(
                    includeAdditionalSchemas(
                      fixPaginationParameter(
                        fixPeriodParameter(swaggerSpec)
                      )
                    )
                  )
                )
              )
            )
          )
        case Failure(e) =>
          logger.error(e.getMessage, e)
          NotFound
      }
    }

  private def sortPaths(spec: JsObject): JsObject = {
    val paths = (spec \ "paths")
      .as[JsObject]
      .fields
      .sortBy(_._1)
      .flatMap { case (path, commands) =>
        JsUtils.toJsValueWrapper(path -> commands)
      }
      .toSeq
    spec + ("paths" -> Json.obj(paths: _*))
  }

  private def includeAdditionalSchemas(spec: JsObject): JsObject = {
    spec.deepMerge(
      Json.obj(
        "components" -> Json.obj(
          "schemas" -> additionalSchemas
        )
      )
    )
  }

  private def fixPaginationParameter(spec: JsObject): JsObject = {
    fixParameterType(
      spec = spec,
      parameterType = "pagination",
      newParameters = List(
        Json.obj(
          "in" -> "query",
          "name" -> "offset",
          "type" -> "integer",
          "required" -> false,
          "default" -> 0
        ),
        Json.obj(
          "in" -> "query",
          "name" -> "limit",
          "type" -> "integer",
          "required" -> false,
          "default" -> 20
        ),
        Json.obj(
          "in" -> "query",
          "name" -> "orderBy",
          "type" -> "string",
          "required" -> false,
          "default" -> ""
        )
      )
    )
  }

  private def fixPeriodParameter(spec: JsObject): JsObject = {
    fixParameterType(
      spec = spec,
      parameterType = "periodfilter",
      newParameters = List(
        Json.obj(
          "in" -> "query",
          "name" -> "period",
          "schema" -> Json.obj(
            "type" -> "string",
            "enum" -> Json.arr(
              "TODAY",
              "YESTERDAY",
              "THIS_WEEK",
              "THIS_MONTH",
              "THIS_YEAR",
              "LAST_24_HOURS",
              "LAST_48_HOURS",
              "LAST_7_DAYS",
              "LAST_30_DAYS",
              "LAST_WEEK",
              "LAST_MONTH",
              "LAST_YEAR"
            )
          ),
          "required" -> false
        ),
        Json.obj(
          "in" -> "query",
          "name" -> "from",
          "type" -> "string",
          "required" -> false
        ),
        Json.obj(
          "in" -> "query",
          "name" -> "to",
          "type" -> "string",
          "required" -> false
        ),
        Json.obj(
          "in" -> "query",
          "name" -> "periodType",
          "schema" -> Json.obj(
            "type" -> "string",
            "enum" -> Json.arr(
              "STARTS_IN",
              "ENDS_IN",
              "ACTIVE_IN",
              "CREATED_IN",
              "UPDATED_IN"
            )
          ),
          "required" -> false
        )
      )
    )
  }

  private def fixParameterType(spec: JsObject, parameterType: String, newParameters: List[JsObject]): JsObject = {
    val fixedPaths = (spec \ "paths")
      .as[JsObject]
      .fields
      .flatMap { case (path, commands) =>
        val fixedCommands = commands
          .as[JsObject]
          .fields
          .flatMap { case (command, definition) =>
            val fixedParameters = (definition \ "parameters").asOpt[List[JsObject]] match {
              case Some(parameters)
                  if parameters.exists(p => (p \ "schema" \ "type").asOpt[String].contains(parameterType)) =>
                parameters
                  .filterNot(parameter =>
                    (parameter \ "schema" \ "type").asOpt[String].contains(parameterType)
                  ) ++ newParameters

              case Some(parameters) =>
                parameters

              case _ =>
                List[JsObject]()
            }
            JsUtils
              .toJsValueWrapper(
                command -> definition
                  .as[JsObject]
                  .deepMerge(Json.obj("parameters" -> fixedParameters))
              )
          }
        JsUtils.toJsValueWrapper(path -> Json.obj(fixedCommands.toSeq: _*))
      }
    spec.deepMerge(Json.obj("paths" -> Json.obj(fixedPaths.toSeq: _*)))
  }

  private def includeRoles(spec: JsObject): JsObject = {
    val fixedPaths = (spec \ "paths")
      .as[JsObject]
      .fields
      .flatMap { case (path, commands) =>
        val fixedCommands = commands
          .as[JsObject]
          .fields
          .flatMap { case (command, definition) =>
            val responses =
              try {
                (definition \ "roles")
                  .as[JsObject]
                  .value("roles")
                  .as[List[String]]
                  .map(Role.withNameInsensitive) match {
                  case Nil => (definition \ "responses").asOpt[JsObject]
                  case list =>
                    val required = (definition \ "roles")
                      .as[JsObject]
                      .value("required")
                      .asOpt[String]
                      .map(_.trim.toUpperCase) match {
                      case Some("ANY") => "Requires ANY of these roles: "
                      case Some("ALL") => "Requires ALL these roles: "
                      case _           => ""
                    }

                    Some(
                      (definition \ "responses")
                        .asOpt[JsObject]
                        .getOrElse(Json.obj()) + ("403" -> Json.obj(
                        "description" -> (required + list
                          .map(_.toString)
                          .mkString(", "))
                      ))
                    )
                }
              } catch {
                case NonFatal(e) =>
                  logger.error(
                    s"Error generating permissions specs: method=$command path=$path",
                    e
                  )
                  throw e
              }
            JsUtils
              .toJsValueWrapper(
                command -> definition
                  .as[JsObject]
                  .deepMerge(Json.obj("responses" -> responses))
              )
          }
        JsUtils.toJsValueWrapper(path -> Json.obj(fixedCommands.toSeq: _*))
      }
    spec.deepMerge(Json.obj("paths" -> Json.obj(fixedPaths.toSeq: _*)))
  }

  private def formatTags(spec: JsObject): JsObject = {
    val fixedPaths = (spec \ "paths")
      .as[JsObject]
      .fields
      .flatMap { case (path, commands) =>
        val fixedCommands = commands
          .as[JsObject]
          .fields
          .flatMap { case (command, definition) =>
            val formattedTags = (definition \ "tags").asOpt[List[String]] match {
              case Some(tags) =>
                tags.map(formatTag)
              case _ =>
                List[String]()
            }
            JsUtils
              .toJsValueWrapper(
                command -> definition
                  .as[JsObject]
                  .deepMerge(Json.obj("tags" -> formattedTags))
              )
          }
        JsUtils.toJsValueWrapper(path -> Json.obj(fixedCommands.toSeq: _*))
      }

    val tags = (spec \ "tags")
      .asOpt[List[JsObject]]
      .getOrElse(List[JsObject]())
      .flatMap(tag => (tag \ "name").asOpt[String])
      .map(formatTag)
      .distinct
      .sorted
      .map(name => Json.obj("name" -> name))

    spec.deepMerge(Json.obj("paths" -> Json.obj(fixedPaths.toSeq: _*), "tags" -> tags))
  }

  private def formatTag(tag: String): String = {
    tag.split('_').map(_.capitalize).mkString(" / ")
  }

}
