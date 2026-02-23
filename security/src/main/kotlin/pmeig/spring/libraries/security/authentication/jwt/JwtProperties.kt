package pmeig.spring.libraries.security.authentication.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import pmeig.spring.libraries.security.ExpirationUpdater
import pmeig.spring.libraries.security.authentication.jwt.model.KeyPair
import java.time.Duration

@ConfigurationProperties("spring.plugins.pmeig.security.auth.jwt")
data class JwtProperties(
  var algorithm: JwtAlgorithm = JwtAlgorithm.ES256,
  override var exp: Duration = Duration.ofHours(4),
  var private: String = "",
  var public: String = "",
  var issuer: String = "",
  var subject: String = "",
  var enabled: Boolean = true
) : ExpirationUpdater("jwt") {

  fun keyPair(): KeyPair {
    val pair = algorithm.create(private, public)
    private.ifEmpty {
      private = pair.private.format
    }
    public.ifEmpty {
      public = pair.public.format
    }
    return pair
  }
}
