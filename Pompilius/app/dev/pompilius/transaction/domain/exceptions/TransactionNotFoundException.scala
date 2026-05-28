package dev.pompilius.transaction.domain.exceptions

import dev.pompilius.shared.domain.VerboseException
import dev.pompilius.transaction.domain.TransactionId

class TransactionNotFoundException(message: String = "Transaction not found") extends VerboseException(message = message)

object TransactionNotFoundException {
  def apply(transactionId:TransactionId): TransactionNotFoundException = {
    new TransactionNotFoundException(s"Transaction with id=${transactionId.toString} not found")
  }
}