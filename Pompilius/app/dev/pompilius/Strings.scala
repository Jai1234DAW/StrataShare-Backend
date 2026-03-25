package dev.pompilius

import scala.util.matching.Regex

object Strings {
  // Cabecera de Nginx
  val N_IPCOUNTRY = "N-IpCountry"

  // Cabecera para Auth alternativas
  val X_User_Id = "X-User-Id"
  val X_Session_Id = "X-Session-Id"

  //Constantes
  val local = "local"
  val dev = "dev"

  //Propiedades y parámetros
  val address = "address"
  val available = "available"
  val availableLanguages = "availableLanguages"
  val avatar = "avatar"
  val baseUrl = "baseUrl"
  val bio = "bio"
  val closeAllSessions = "closeAllSessions"
  val code = "code"
  val contentType = "contentType"
  val country = "country"
  val countryOverride = "countryOverride"
  val created = "created"
  val createdAt = "createdAt"
  val currentDateTime = "currentDateTime"
  val debugId = "debugId"
  val deleted = "deleted"
  val description = "description"
  val email = "email"
  val enabled = "enabled"
  val environment = "environment"
  val error = "error"
  val fingerprint = "fingerprint"
  val file = "file"
  val filename = "filename"
  val firstName = "firstName"
  val fullName = "fullName"
  val id = "id"
  val isPublic = "isPublic"
  val mailSent = "mailSent"
  val mailType = "mailType"
  val metadata = "metadata"
  val newPassword = "newPassword"
  val node = "node"
  val nodeId = "nodeId"
  val notes = "notes"
  val language = "language"
  val lastName = "lastName"
  val limit = "limit"
  val logAllExceptions = "logAllExceptions"
  val logAllRequests = "logAllRequests"
  val offset: String = "offset"
  val oldPassword = "oldPassword"
  val orderBy = "orderBy"
  val password = "password"
  val parameters = "parameters"
  val phone = "phone"
  val relativePath = "relativePath"
  val role = "role"
  val roles = "roles"
  val search="search"
  val sentAt = "sentAt"
  val sessionId = "sessionId"
  val size = "size"
  val token = "token"
  val updated = "updated"
  val updatedAt = "updatedAt"
  val uploadURL = "uploadUrl"
  val user = "user"
  val userId = "userId"
  val username = "username"

  val useSSL = "useSSL"
  val valid = "valid"
  val variants = "variants"

  // Regex
  val signedTokenRegex: Regex = """^(e[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+)$""".r
  val usernameRegex: Regex = """^([a-zA-Z0-9\_]+)$""".r
  val urlSafeRegex: Regex = """^([\p{L}0-9-._~]+)$""".r
  val emailRegex: Regex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".r
}
