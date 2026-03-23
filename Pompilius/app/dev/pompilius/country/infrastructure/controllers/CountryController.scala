package dev.pompilius.country.infrastructure.controllers

import dev.pompilius.country.domain.Country
import dev.pompilius.country.infrastructure.writers.CountryWriter
import dev.pompilius.shared.domain.{Configuration, Paginated, Pagination}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import play.api.cache.AsyncCacheApi
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

@Singleton
class CountryController @Inject()(
    paginatedWriter: PaginatedWriter,
    countryWriter: CountryWriter,
    cache: AsyncCacheApi,
    configuration: Configuration
)(implicit ec: ExecutionContext)
    extends InjectedController
    with BaseController {

  def getCountries: Action[AnyContent] =
    Action.async {
      cache
        .getOrElseUpdate("countries", Duration.Inf) {
          val countries =
            configuration.countries.featured ++ (Country.values.toList diff configuration.countries.featured)
          paginatedWriter.toJson(Paginated(countries, Pagination.all))(countryWriter.toJson)
        }
        .map(Ok(_))
    }
}
