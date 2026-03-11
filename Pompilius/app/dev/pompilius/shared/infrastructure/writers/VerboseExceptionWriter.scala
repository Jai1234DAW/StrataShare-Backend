package dev.pompilius.shared.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import play.mvc.Http.HeaderNames
//import dev.pompilius.auth.domain.exceptions.InvalidPasswordOrUsernameException
//Colocar aqui las excepciones que faltan
//import dev.pompilius.image.domain.exceptions.{ImageNotFoundException, ImageStorageException}
//import dev.pompilius.mail.domain.exceptions.SendMailException
//import dev.pompilius.page.domain.exceptions.PageNotFoundException
import dev.pompilius.shared.domain.VerboseException
import dev.pompilius.shared.domain.exceptions._
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
//import dev.pompilius.user.domain.exceptions._
import play.api.libs.json._
import play.api.mvc.{Result, Results}

import javax.inject.{Inject, Singleton}

@ImplementedBy(classOf[VerboseExceptionWriterImpl])
trait VerboseExceptionWriter {
  def toJson(exception: VerboseException): JsValue
  def toResult(exception: VerboseException): Result
  def code(exception: VerboseException): Int
  def status(exception: VerboseException): Results.Status
}

@Singleton
class VerboseExceptionWriterImpl @Inject() extends VerboseExceptionWriter {

  override def toJson(exception: VerboseException): JsValue = {
    Json.toJson(
      Json
        .obj(
          List(
            toJsValueWrapper(Strings.code, code(exception)),
            toJsValueWrapper(Strings.error, exception.message),
            toJsValueWrapper(Strings.debugId, exception.id)
          ).flatten: _*
        )
    )
  }

  override def toResult(exception: VerboseException): Result = {
    exception match {
      case e: TooManyRequestsException =>
        status(exception)(Json.toJson(toJson(exception)))
          .withHeaders(HeaderNames.RETRY_AFTER -> e.retryAfter.toString)
      case _ =>
        status(exception)(Json.toJson(toJson(exception)))
    }

  }

  override def code(exception: VerboseException): Int = {
    exception match {
      case _: TooManyRequestsException => 1001
      case _: BadRequestException      => 1000
      case _: ForbiddenException       => 2000
      //case _: PageNotFoundException              => 3016
      //case _: AccountMemberNotFoundException     => 3015
      //case _: AccountNotFoundException           => 3014
      //case _: FormNotFoundException              => 3010
      //case _: ImageNotFoundException             => 3003
      //case _: RoleNotFoundException              => 3002
      //case _: UserNotFoundException              => 3001
      case _: NotFoundException => 3000
      //case _: InvalidPasswordOrUsernameException => 4001
      //case _: UnauthorizedException              => 4000
      //case _: AccountNameAlreadyInUseException   => 6013
      //case _: RoleInUseException                 => 6004
      //case _: RoleNameAlreadyInUseException      => 6003
      //case _: EmailAlreadyInUseException         => 6002
      //case _: UsernameAlreadyInUseException      => 6001
      case _: UnprocessableException => 6000
      case _: GoneException          => 7000
      //case _: SendMailException                  => 5008
      //case _: ImageStorageException              => 5002
      case _: InternalServerException => 5001
      case _                          => 5000
    }
  }

  override def status(exception: VerboseException): Results.Status = {
    exception match {
      case _: TooManyRequestsException => Results.TooManyRequests
      case _: BadRequestException      => Results.BadRequest
      case _: ForbiddenException       => Results.Forbidden
      //case _: PageNotFoundException              => Results.NotFound
      //case _: FormNotFoundException              => Results.NotFound
      //case _: AccountMemberNotFoundException     => Results.NotFound
      //case _: AccountNotFoundException           => Results.NotFound
      //case _: ImageNotFoundException             => Results.NotFound
      //case _: RoleNotFoundException              => Results.NotFound
      //case _: UserNotFoundException              => Results.NotFound
      case _: NotFoundException => Results.NotFound
      //case _: InvalidPasswordOrUsernameException => Results.Unauthorized
      case _: UnauthorizedException => Results.Unauthorized
      case _: GoneException         => Results.Gone
      //case _: AccountNameAlreadyInUseException   => Results.UnprocessableEntity
      //case _: RoleInUseException                 => Results.UnprocessableEntity
      //case _: RoleNameAlreadyInUseException      => Results.UnprocessableEntity
      //case _: EmailAlreadyInUseException         => Results.UnprocessableEntity
      //case _: UsernameAlreadyInUseException      => Results.UnprocessableEntity
      case _: UnprocessableException => Results.UnprocessableEntity
      //case _: SendMailException                  => Results.InternalServerError
      //case _: ImageStorageException              => Results.InternalServerError
      case _: InternalServerException => Results.InternalServerError
      case _                          => Results.InternalServerError
    }
  }
}
