package dev.pompilius.users.infrastructure.repositories

import dev.pompilius.Strings
import dev.pompilius.country.domain.Country
import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.exceptions.UserNotFoundException
import dev.pompilius.users.domain.{User, UserFilter, UserId, UserRepository}
import org.apache.pekko.Done
import play.api.i18n.Lang
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.Try

@Singleton
class UserMySqlRepository @Inject() (
)(implicit dbExecutionContext: DbExecutionContext)
    extends UserRepository
    with SQLSyntaxSupport[User] {

  override val tableName = "users"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(u: SyntaxProvider[User])(rs: WrappedResultSet): User =
    apply(u.resultName)(rs)
  def apply(u: ResultName[User])(rs: WrappedResultSet): User =
    User(
      id = UserId(rs.get[Long](u.id)),
      username = rs.get(u.username),
      passwordHash = rs.get(u.passwordHash),
      country = Country.withNameInsensitiveOption(rs.get[String](u.country)).getOrElse(Country.XX),
      enabled = rs.get(u.enabled),
      email = rs.get(u.email),
      firstName = rs.get(u.firstName),
      lastName = rs.get(u.lastName),
      phone = rs.get(u.phone),
      created = rs.get(u.created),
      updated = rs.get(u.updated),
      avatar = rs.get[Option[Long]](u.avatar).map(AttachmentId(_)),
      language = rs.get[Option[String]](u.language).flatMap(lang => Try(Lang(lang)).toOption),
      notes = rs.get(u.notes),
      bio = rs.get(u.bio)
    )

  private val u = this.syntax("u")

  override def getById(userId: UserId): Future[User] = {
    findById(userId).map(_.getOrElse(throw new UserNotFoundException(s"User with id ${userId.toString} not found")))
  }

  override def findById(userId: UserId): Future[Option[User]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as u).where.eq(u.id, userId.id)
        }.map(apply(u.resultName)(_)).single()
      }
    }

  override def findByUsername(username: String): Future[Option[User]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as u).where.eq(u.username, username)
        }.map(apply(u.resultName)(_)).single()
      }
    }

  override def findByEmail(email: String): Future[Option[User]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as u).where.eq(u.email, email)
        }.map(apply(u.resultName)(_)).single()
      }
    }

  override def find(filter: UserFilter, pag: Pagination): Future[List[User]] =
    Future {
      val usernameFilter: Option[SQLSyntax] = filter.username.map(username => sqls.eq(u.username, username))
      val firstNameFilter: Option[SQLSyntax] = filter.firstName.map(firstName => sqls.eq(u.firstName, firstName))
      val lastNameFilter: Option[SQLSyntax] = filter.lastName.map(lastName => sqls.eq(u.lastName, lastName))
      val enabledFilter: Option[SQLSyntax] = filter.enabled.map(enabled => sqls.eq(u.enabled, enabled))
      val countryFilter: Option[SQLSyntax] = filter.country.map(country => sqls.eq(u.country, country.toString))

      val searchFilter: Option[SQLSyntax] = filter.search.map(buildSearchFilter)

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as u)
            .append(
              List(
                usernameFilter,
                firstNameFilter,
                lastNameFilter,
                enabledFilter,
                countryFilter,
                searchFilter
              ).flatten match {
                case l if l.nonEmpty =>
                  sqls.where.append(sqls.joinWithAnd(l: _*))
                case _ =>
                  sqls.empty
              }
            )
            .orderBy(u.id)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(u.resultName)(_)).list()
      }
    }

  private def buildSearchFilter(search: String): SQLSyntax = {
    search.split(":") match {
      case Array(Strings.id, id) =>
        Try(UserId(id.trim)).map(userId => sqls.eq(u.id, userId.id)).getOrElse(defaultSearchFilter(search))
      case Array(Strings.username, username) =>
        val normalizedSearch = ("%" + username.trim + "%").replaceAll("( |%)+", "%")
        sqls.like(u.username, normalizedSearch)
      case Array(Strings.firstName, firstName) =>
        val normalizedSearch = ("%" + firstName.trim + "%").replaceAll("( |%)+", "%")
        sqls.like(u.firstName, normalizedSearch)
      case Array(Strings.lastName, lastName) =>
        val normalizedSearch = ("%" + lastName.trim + "%").replaceAll("( |%)+", "%")
        sqls.like(u.lastName, normalizedSearch)
      case Array(Strings.email, email) =>
        val normalizedSearch = ("%" + email.trim + "%").replaceAll("( |%)+", "%")
        sqls.like(u.email, normalizedSearch)
      case Array(Strings.phone, phone) =>
        val normalizedSearch = ("%" + phone.trim + "%").replaceAll("( |%)+", "%")
        sqls.like(u.phone, normalizedSearch)
      case Array(Strings.country, country) =>
        sqls.eq(u.country, country.trim)
      case _ =>
        defaultSearchFilter(search)
    }
  }

  private def defaultSearchFilter(search: String): SQLSyntax = {
    val normalizedSearch = ("%" + search + "%").replaceAll("( |%)+", "%")
    sqls.roundBracket(
      sqls
        .like(u.username, normalizedSearch)
        .or
        .like(u.firstName, normalizedSearch)
        .or
        .like(u.lastName, normalizedSearch)
    )
  }

  override def save(user: User): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> user.id.id,
          column.username -> user.username,
          column.passwordHash -> user.passwordHash,
          column.email -> user.email,
          column.phone -> user.phone,
          column.enabled -> user.enabled,
          column.firstName -> user.firstName,
          column.lastName -> user.lastName,
          column.country -> user.country.toString,
          column.language -> user.language.map(_.language),
          column.notes -> user.notes,
          column.bio -> user.bio,
          column.created -> user.created,
          column.updated -> user.updated,
          column.avatar -> user.avatar.map(_.id)
        )

        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.id, values: _*))
        }.update()
      }
      Done

    }


}
