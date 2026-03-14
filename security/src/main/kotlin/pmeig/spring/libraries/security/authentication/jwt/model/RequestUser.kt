package pmeig.spring.libraries.security.authentication.jwt.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serial

open class RequestUser(
  pseudo: String = "",
  credential: String = ""
): HashMap<String, Any>() {
  @get:JsonIgnore
  var pseudo: String
    get() = getOrDefault("pseudo", "") as String
    set(value) { put("pseudo", value) }
  @get:JsonIgnore
  var credential: String
    get() = getOrDefault("credential", "") as String
    set(value) { put("credential", value) }

  init {
    this.pseudo = pseudo
    this.credential = credential
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = -6023570230307286217L
  }
}
