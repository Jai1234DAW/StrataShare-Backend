package dev.pompilius.barter.domain.request

case class MailBarterRequest(
    token: String,
    transactionId: String,
    email: String,
    barterId: String
)
