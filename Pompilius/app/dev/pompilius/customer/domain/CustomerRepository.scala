package dev.pompilius.customer.domain

import com.google.inject.ImplementedBy
import dev.pompilius.customer.infrastructure.repositories.mysql.CustomerMySqlRepository
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerMySqlRepository])
trait CustomerRepository {

  def findByUserId(userId: UserId, gateway: Gateway): Future[Option[Customer]]

  def getAllByUserId(userId: UserId): Future[List[Customer]]

  def save(customer: Customer): Future[Done]

}
