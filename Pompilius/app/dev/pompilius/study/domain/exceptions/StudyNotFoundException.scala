package dev.pompilius.study.domain.exceptions

import dev.pompilius.shared.domain.exceptions.NotFoundException

class StudyNotFoundException(message: String = "Not Found") extends NotFoundException

