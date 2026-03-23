package dev.pompilius.mail.domain

import com.google.inject.ImplementedBy

import scala.concurrent.Future

@ImplementedBy(classOf[SendMailQueueImpl])
trait SendMailQueue {

  def add(mail: Mail, userId: Option[userId], key: Option[String]): Unit
  def getSize: Future[Int]
  def getKeys(limit: Int): Future[Seq[String]]
  def flush(): Unit

}
