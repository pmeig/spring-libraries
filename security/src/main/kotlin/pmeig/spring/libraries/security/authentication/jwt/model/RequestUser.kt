package pmeig.spring.libraries.security.authentication.jwt.model

@Suppress("unused")
open class RequestUser(
  val pseudo: String,
  val credential: String,
  val additional: Map<String, Any> = mapOf()
)
