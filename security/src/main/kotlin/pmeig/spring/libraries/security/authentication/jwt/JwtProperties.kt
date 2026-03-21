package pmeig.spring.libraries.security.authentication.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.security.ExpirationUpdater
import pmeig.spring.libraries.security.authentication.jwt.model.KeyPair
import java.time.Duration

@Suppress("unused")
@Configuration
@ConfigurationProperties("spring.security.pmeig.auth.jwt")
class JwtProperties(
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
