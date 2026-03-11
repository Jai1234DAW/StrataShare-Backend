package dev.pompilius.shared.infrastructure

import dev.pompilius.shared.domain.exceptions.{ForbiddenException, TooManyRequestsException, UnauthorizedException}
import dev.pompilius.shared.domain.{Clock, Configuration,Pagination}
import dev.pompilius.user.domain._
import dev.pompilius.auth.domain.{Session, SessionId, SessionRepository, SessionValidator}
import dev.pompilius.Strings
import dev.pompilius.shared.infrastructure.binders.PaginationBinder
import org.joda.time.DateTime
import play.api.cache.AsyncCacheApi
import play.api.http.HeaderNames
import play.api.i18n.{Lang, Langs}
import play.api.libs.json.Json
import play.api.mvc.{
  ActionBuilder,
  AnyContent,
  AnyContentAsJson,
  InjectedController,
  QueryStringBindable,
  Request,
  Result
}

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait BaseController extends InjectedController {

  private[this] var _config: Configuration = _

  @SuppressWarnings(Array("NullParameter"))
  def configuration: Configuration = {
    if (_config != null) _config
    else {
      throw new NoSuchElementException(
        "Config not set! Call setConfiguration or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def setConfiguration(config: Configuration): Unit = {
    _config = config
  }

  private[this] var _clock: Clock = _

  @SuppressWarnings(Array("NullParameter"))
  def clock: Clock = {
    if (_clock != null) _clock
    else {
      throw new NoSuchElementException(
        "clock not set! Call setClock or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def setClock(c: Clock): Unit = {
    _clock = c
  }

  private[this] var _cache: AsyncCacheApi = _

  @SuppressWarnings(Array("NullParameter"))
  def cache: AsyncCacheApi = {
    if (_cache != null) _cache
    else {
      throw new NoSuchElementException(
        "Cache not set! Call setCache or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def setCache(c: AsyncCacheApi): Unit = {
    _cache = c
  }

  private[this] var _langs: Langs = _

  @SuppressWarnings(Array("NullParameter"))
  def langs: Langs = {
    if (_langs != null) _langs
    else
      throw new NoSuchElementException(
        "Langs not set! Call setLangs or create the instance with dependency injection."
      )
  }

  @Inject
  def setLangs(l: Langs): Unit = {
    _langs = l
  }

  def getLanguage[A](implicit request: Request[A]): Lang = {
    langs.preferred(request.acceptLanguages.flatMap(lang => Try(Lang(lang.language)).toOption))
  }

  private[this] var _sessionValidator: SessionValidator = _

  @SuppressWarnings(Array("NullParameter"))
  def sessionValidator: SessionValidator = {
    if (_sessionValidator != null) _sessionValidator
    else
      throw new NoSuchElementException(
        "SessionValidator not set! Call setSessionValidator or create the instance with dependency injection."
      )
  }

  @Inject
  def setSessionValidator(sv: SessionValidator): Unit = {
    _sessionValidator = sv
  }

  private[this] var _userRepository: UserRepository = _

  @SuppressWarnings(Array("NullParameter"))
  def userRepository: UserRepository = {
    if (_userRepository != null) _userRepository
    else {
      throw new NoSuchElementException(
        "UserRepository not set! Call setUserRepository or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def setUserRepository(ur: UserRepository): Unit = {
    _userRepository = ur
  }

  //QUEDE AQUI
  private[this] var _userRoleRepository: UserRoleRepository = _

  @SuppressWarnings(Array("NullParameter"))
  def userRoleRepository: UserRoleRepository = {
    if (_userRoleRepository != null) _userRoleRepository
    else {
      throw new NoSuchElementException(
        "UserRoleRepository not set! Call setUserRoleRepository or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def setUserRoleRepository(ur: UserRoleRepository): Unit = {
    _userRoleRepository = ur
  }

  private[this] var _sessionRepository: SessionRepository = _

  @SuppressWarnings(Array("NullParameter"))
  def sessionRepository: SessionRepository = {
    if (_sessionRepository != null) _sessionRepository
    else {
      throw new NoSuchElementException(
        "SessionRepository not set! Call setSessionRepository or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def setSessionRepository(sr: SessionRepository): Unit = {
    _sessionRepository = sr
  }

  // Obtiene y valida la sesión actual de usuario, o None si el usuario no ha iniciado sesión
  def findCurrentSession[A](implicit
      request: Request[A],
      ec: ExecutionContext
  ): Future[Option[Session]] = {

    (
      request.session
        .get(Strings.sessionId)
        .orElse(request.headers.get(CustomHeaderNames.X_SESSION_ID))
        .flatMap(id => Try(SessionId(id)).toOption)
      ) match {
      case (Some(sessionId)) =>
        sessionRepository.findById(sessionId).map {
          case Some(session)
              if !session.deleted &&
                session.created.plusSeconds(configuration.auth.maxAge.toSeconds.toInt).isAfter(clock.now) =>
            Some(session)
          case _ =>
            None
        }
      case _ =>
        Future.successful(None)
    }
  }

  //Busca y valida el usuario actual a partir de la sesión activa.
  //Si existe una sesión válida y el usuario está habilitado, retorna Some(user).
  //Si no hay sesión, el usuario no existe o está deshabilitado, retorna None.

  def findCurrentUser[A](implicit
      request: Request[A],
      ec: ExecutionContext
  ): Future[Option[User]] = {
    findCurrentSession.flatMap {
      case Some(session) =>
        userRepository.findById(session.userId).map {
          case Some(user) if user.enabled =>
            Some(user)
          case _ =>
            None
        }
      case _ =>
        Future.successful(None)
    }
  }

  def withAuthenticatedUser[T](
      f: (Session, User, List[Role]) => Future[Result]
  )(implicit request: Request[T], ec: ExecutionContext): Future[Result] = {
    for {
      session <- findCurrentSession.map {
        _.getOrElse(throw new UnauthorizedException("Unauthorized"))
      }
      user <- _userRepository.findById(session.userId).map {
        _.getOrElse(throw new UnauthorizedException("Unauthorized"))
      }

      _ = if (!user.enabled) throw new UnauthorizedException("Unauthorized")

      userRoles <- userRoleRepository.getAllByUserId(session.userId).map(_.map(_.role))
      result <- f(session, user, userRoles)
    } yield {
      result
    }
  }

//  private def updatePersonLanguagePreference[A](
//      user: User
//  )(implicit request: Request[A], ec: ExecutionContext): Future[Unit] = {
//    user.id match {
//      case Some(id: UserId) =>
//        getSelectedLanguage(request).map(_.language) match {
//          // Solo actualizamos el idioma si el usuario ha seleccionado uno explícitamente
//          case Some(lang) =>
//            personRepository.findById(personId).flatMap {
//              case Some(person) if !person.language.exists(_.language == lang) =>
//                val updatedPerson = person.copy(language = Some(Lang(lang)), updatedAt = clock.now)
//                personRepository.updateIfNotModified(updatedPerson, person.updatedAt).map(_ => ())
//              case _ =>
//                Future.successful(())
//            }
//          case _ =>
//            Future.successful(())
//        }
//      case _ =>
//        Future.successful(())
//    }
//  }

  //MIRAR SI ESTO SERÁ NECESARIO
  def findRolesByUser(user: User)(implicit
      ec: ExecutionContext
  ): Future[List[Role]] = {
    userRoleRepository
      .find(UserRoleFilter(userId = Some(user.id)), Pagination.all)
      .map(_.map(_.role).distinct)
  }

  // Se utiliza para asegurarnos de que la función que se pasa como parámetro solo
  // se ejecuta si el usuario tiene TODOS los roles requeridos según el parámetro "roles"
  def withThisRoles[T](roles: Set[Role])(
      f: (Session, User, List[Role]) => Future[Result]
  )(implicit request: Request[T], ec: ExecutionContext): Future[Result] = {
    withAuthenticatedUser {
      case (session, user, userRoles) =>
        if (roles.forall(userRoles.contains)) f(session, user, userRoles)
        else {
          throw new ForbiddenException("Forbidden")
        }
    }
  }

  // Se utiliza para asegurarnos de que la función que se pasa como parámetro solo
  // se ejecuta si el usuario tiene ALGUNO los roles requeridos. A la función se le pasa el primer
  // rol de la lista que coincide con los que tiene el usuario
  def withAnyOfThisRoles[T](roles: Seq[Role])(
      f: (Session, User, List[Role], Role) => Future[Result]
  )(implicit request: Request[T], ec: ExecutionContext): Future[Result] = {
    withAuthenticatedUser {
      case (session, user, userRoles) =>
        roles.find(userRoles.contains) match {
          case Some(role) =>
            f(session, user, userRoles, role)
          case _ =>
            throw new ForbiddenException("Forbidden")
        }
    }
  }

  def remoteAddress[A](implicit request: Request[A]): String = {
    request.remoteAddress
  }

  // Se utiliza para limitar el número de peticiones que un usuario puede hacer en un periodo de tiempo
  def withLimit[T](maxRequest: Int, timeWindow: FiniteDuration, key: Option[String])(
      f: () => Future[Result]
  )(implicit request: Request[T], ec: ExecutionContext): Future[Result] = {
    val cacheKey =
      s"Requests::${key.getOrElse(s"${request.method}::${request.path}")}::${request.remoteAddress}::${DateTime.now.getMillis / timeWindow.toMillis}"
    for {
      _ <-
        cache
          .get[Int](cacheKey)
          .map {
            case Some(count) if count >= maxRequest =>
              throw new TooManyRequestsException(retryAfter = timeWindow.toSeconds)
            case value =>
              cache.set(cacheKey, value.getOrElse(0) + 1, timeWindow)
          }
          .recoverWith {
            case _: NoSuchElementException =>
              cache.set(cacheKey, 1, timeWindow)
          }
      result <- f()
    } yield {
      result
    }
  }

  val paginationBinder: QueryStringBindable[Pagination] = PaginationBinder.paginationBinder

  def getOrderBy(implicit request: Request[_]): Seq[String] = {
    paginationBinder
      .bind(
        "",
        request.queryString
      )
      .flatMap {
        case Right(pagination) =>
          Some(pagination.orderBy)
        case _ =>
          None
      }
      .getOrElse(Seq.empty)
  }

}
