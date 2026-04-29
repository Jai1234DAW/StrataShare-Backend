package dev.pompilius.barter.domain.request

case class AcceptMailBarterRequest(
    token: String,
    transactionId: String,
    email: String
)
