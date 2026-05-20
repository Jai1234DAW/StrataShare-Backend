package dev.pompilius.userStatistics.domain

import dev.pompilius.users.domain.UserId

case class UserStatistics(
    id: UserStatisticsId,
    userId: UserId,
    totalFollowers: Int = 0,
    totalPurchases: Int = 0,
    totalSales: Int = 0,
    totalBarters: Int = 0,
    totalOffers: Int = 0,
    mostSoldResource: Option[String],
    mostBarteredResource: Option[String],
    mostOfferedResource: Option[String],
    totalResourcesUploaded: Int = 0,
    dateRange: String
)
