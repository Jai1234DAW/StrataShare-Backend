package dev.pompilius.resource.domain

import dev.pompilius.resource.domain.sample.Sample
import dev.pompilius.resource.domain.study.Study
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

trait ResourceRepository {

  def findById(id: ResourceId): Future[Option[Resource]]

  //Necesario para validar que un recurso pertenece a un usuario, por ejemplo,
  // para mostrarlo en su perfil o para validar que el usuario tiene permiso de editarlo o venderlo
  def findByIdAndOwner(id: ResourceId, ownerId: UserId): Future[Option[Resource]]

  def find(filter: ResourceFilter, pagination: Pagination): Future[List[Resource]]

  def save(resource: Resource, sample:Option[Sample], study: Option[Study]): Future[Done]

  def delete(resourceId: ResourceId): Future[Done]
}
