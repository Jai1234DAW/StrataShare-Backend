package dev.pompilius.shared.domain

import org.apache.pekko.http.scaladsl.model.DateTime


trait Clock {
  def now():DateTime
  def startOfDay(): DateTime=now
}