package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class GoneException(message: String = "Expired") extends VerboseException(message = message)