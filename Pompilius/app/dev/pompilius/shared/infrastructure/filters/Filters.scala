package dev.pompilius.shared.infrastructure.filters

import play.api.http.{DefaultHttpFilters, EnabledFilters}

import javax.inject.{Inject, Singleton}

@Singleton
class Filters @Inject()(
      defaultFilters:EnabledFilters,
cacheControlFilter: CacheControlFilter,
nodeFilter: NodeFilter,
dateFilter: DateFilter           )
) extends DefaultHttpFilters(
  defaultFilters.filters :+ cacheControlFilter :+ nodeFilter :+ dateFilter: _*
)