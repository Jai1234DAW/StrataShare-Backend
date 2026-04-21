package dev.pompilius.review.infrastructure.controllers

import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain.{ResourceId, ResourceRepository, ResourceUserRepository, ResourceUserType}
import dev.pompilius.review.domain.exceptions.ReviewNotFoundException
import dev.pompilius.review.domain.{Review, ReviewId, ReviewRepository}
import dev.pompilius.review.infrastructure.parsers.{CreateReviewRequestParser, UpdateReviewRequestParser}
import dev.pompilius.review.infrastructure.writers.ReviewWriter
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException}
import dev.pompilius.shared.domain.{Clock, Paginated, Pagination}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.users.domain.{Role, UserRepository}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class ReviewController @Inject() (
    reviewRepository: ReviewRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    userRepository: UserRepository,
    reviewWriter: ReviewWriter,
    paginatedWriter: PaginatedWriter,
    clock: Clock
)(implicit val ec: ExecutionContext)
    extends BaseController {

  /* Crear una review para un recurso. Solo puede hacer review si ha comprado/recibido el recurso */

  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL, Role.AMATEUR)) {
        case (_, user, _, _) =>
          val createRequest = CreateReviewRequestParser.parse(request)
          val resourceId = ResourceId(createRequest.resourceId)
          for {

            // Verificar que el recurso existe
            resource <-
              resourceRepository
                .findById(resourceId)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $resourceId not found")))
            // Verificar que el usuario tiene acceso al recurso (lo compró o recibió)
            resourceUser <-
              resourceUserRepository
                .findByResourceAndUser(resourceId, user.id)
                .map(_.getOrElse(throw new ForbiddenException("You must own this resource to review it")))
            _ = if (resourceUser.deleted) {
              throw new ForbiddenException("You no longer have access to this resource")
            }
            _ = if (resourceUser.resourceUserType == ResourceUserType.OWNER) {
              throw new ForbiddenException("You cannot review your own resource")
            }
            // Verificar que no tiene ya una review
            existingReview <- reviewRepository.findByResourceAndUser(resourceId, user.id)
            _ = if (existingReview.isDefined) {
              throw new BadRequestException("You already reviewed this resource")
            }
            // Validar rating
            _ = if (createRequest.rating < 1 || createRequest.rating > 5) {
              throw new BadRequestException("Rating must be between 1 and 5")
            }
            // Crear review
            reviewId = ReviewId.gen(configuration.nodeId)
            review = Review(
              id = reviewId,
              resourceId = resourceId,
              userId = user.id,
              rating = createRequest.rating,
              comment = createRequest.comment,
              createdAt = clock.now,
              updatedAt = clock.now
            )
            _ <- reviewRepository.save(review)
            json <- reviewWriter.asJsonWithUsername(review, user.username)
          } yield Ok(json)
      }
    }

  /* Actualizar una review existente */
  def update(reviewId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ReviewId(reviewId)
          val updateRequest = UpdateReviewRequestParser.parse(request)
          for {

            // Obtener review
            review <-
              reviewRepository
                .findById(rid)
                .map(_.getOrElse(throw new ReviewNotFoundException(s"Review $reviewId not found")))
            // Verificar que es el autor de la review
            _ = if (review.userId != user.id) {
              throw new ForbiddenException("You can only update your own reviews")
            }

            // Actualizar campos
            updatedReview = review.copy(
              rating = updateRequest.rating.getOrElse(review.rating),
              comment = updateRequest.comment.orElse(review.comment),
              updatedAt = clock.now
            )

            _ <- reviewRepository.save(updatedReview)
            json <- reviewWriter.asJsonWithUsername(updatedReview, user.username)
          } yield Ok(json)
      }
    }

  /* Eliminar una review */

  def delete(reviewId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ReviewId(reviewId)
          for {
            review <-
              reviewRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Review $reviewId not found")))
            _ = if (review.userId != user.id) {
              throw new ForbiddenException("You can only delete your own reviews")
            }
            _ <- reviewRepository.delete(rid)
          } yield Ok
      }
    }

  /* Obtener todas las reviews de un recurso (con paginación) */
  def getByResource(resourceId: String, pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ResourceId(resourceId)
          for {

            // Verificar que el recurso existe
            _ <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $resourceId not found")))

            // Obtener reviews
            reviews <- reviewRepository.findByResource(rid, pag.oneMore)

            // Generar JSON paginado con nombres de usuario
            json <- paginatedWriter.toJson(Paginated(reviews, pag)) { review =>
              for {
                user <-
                  userRepository
                    .findById(review.userId)
                    .map(_.map(_.username).getOrElse("Unknown"))
                reviewJson <- reviewWriter.asJsonWithUsername(review, user)
              } yield reviewJson
            }
          } yield Ok(json)
      }
    }

  /* Obtener estadísticas de reviews de un recurso (promedio y total) */

  def getResourceStats(resourceId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ResourceId(resourceId)
          for {
            _ <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $resourceId not found")))
            avgRating <- reviewRepository.getAverageRating(rid).map(_.getOrElse(0.0))
            count <- reviewRepository.getReviewCount(rid)
          } yield Ok(
            Json.obj(
              "resourceId" -> resourceId,
              "averageRating" -> avgRating,
              "reviewCount" -> count
            )
          )
      }
    }

  /* Obtener la review del usuario para un recurso específico */

  def getMyReview(reviewId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ReviewId(reviewId)

          for {
            reviewOpt <- reviewRepository.findById(rid)
            json <- reviewOpt match {
              case Some(review) if review.userId == user.id =>
                reviewWriter.asJson(review)
              case _ =>
                throw new ReviewNotFoundException("Review not found")
            }
          } yield Ok(json)
      }
    }
}
