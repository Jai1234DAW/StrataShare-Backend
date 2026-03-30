package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.resource.domain.{Resource, ResourceFilter, ResourceId, ResourceRepository, SampleResource, StudyResource}
import dev.pompilius.resource.domain.sample.SampleRepository
import dev.pompilius.resource.domain.study.StudyRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Repositorio agregador que coordina llamadas a SampleRepository y StudyRepository
 * Implementa el patrón Facade para operaciones en conjunto
 */
@Singleton
class ResourceRepositoryAggregate @Inject() (
    sampleRepository: SampleRepository,
    studyRepository: StudyRepository
)(implicit ec: ExecutionContext)
    extends ResourceRepository {

  /**
   * Obtiene un recurso por ID (puede ser Sample o Study)
   */
  override def findById(id: ResourceId): Future[Option[Resource]] =
    for {
      sampleOpt <- sampleRepository.findById(id)
      result <- sampleOpt match {
        case Some(sample) => Future.successful(Some(sample: Resource))
        case None =>
          for {
            studyOpt <- studyRepository.findById(id)
          } yield studyOpt.map(s => s: Resource)
      }
    } yield result

  /**
   * Obtiene un recurso por ID y propietario
   */
  override def findByIdAndOwner(id: ResourceId, ownerId: UserId): Future[Option[Resource]] = {
    // Esto depende de cómo manejes la propiedad en tu BD
    // Por ahora solo retorna el recurso
    findById(id)
  }

  /**
   * Busca recursos con filtro (puede ser Sample o Study)
   */
  override def find(filter: ResourceFilter, pagination: Pagination): Future[Seq[Resource]] = {
    // Esto depende de tu implementación específica
    // Por ahora retorna secuencia vacía
    Future.successful(Seq.empty)
  }

  /**
   * Guarda un recurso (Sample o Study)
   */
  override def save(resource: Resource): Future[Done] =
    resource match {
      case sample: SampleResource =>
        sampleRepository.save(sample)
      case study: StudyResource =>
        studyRepository.save(study)
    }

  /**
   * Borra un recurso
   */
  override def delete(resourceId: ResourceId): Future[Done] = {
    // Esto es más complejo porque necesitas saber qué tipo es
    // Una opción es solo marcar como deleted en la BD
    Future.successful(Done)
  }

  /**
   * Obtiene todos los recursos de un usuario (Samples + Studies)
   */
  override def findAllByUser(userId: UserId): Future[Seq[Resource]] =
    for {
      samples <- sampleRepository.findAllByUser(userId)
      studies <- studyRepository.findAllByUser(userId)
    } yield {
      val allResources: Seq[Resource] = samples ++ studies
      allResources
    }

  /**
   * Obtiene todos los recursos con paginación (Samples + Studies)
   */
  override def findAll(pag: Pagination): Future[Seq[Resource]] =
    for {
      samples <- sampleRepository.findAll(pag)
      studies <- studyRepository.findAll(pag)
    } yield {
      val allResources: Seq[Resource] = samples ++ studies
      allResources
    }
}

