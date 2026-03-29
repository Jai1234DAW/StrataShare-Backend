package dev.pompilius.resource.domain

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.sample.Sample
import dev.pompilius.resource.domain.study.Study
import dev.pompilius.resource.infrastructure.repositories.ResourceMySqlRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[ResourceMySqlRepository])
trait ResourceRepository {

  def findById(id: ResourceId): Future[Option[Resource]]

  //Necesario para validar que un recurso pertenece a un usuario, por ejemplo,
  // para mostrarlo en su perfil o para validar que el usuario tiene permiso de editarlo o venderlo
  def findByIdAndOwner(id: ResourceId, ownerId: UserId): Future[Option[Resource]]

  def find(filter: ResourceFilter, pagination: Pagination): Future[List[Resource]]

  def save(resource: Resource): Future[Done]

  def delete(resourceId: ResourceId): Future[Done]
}
