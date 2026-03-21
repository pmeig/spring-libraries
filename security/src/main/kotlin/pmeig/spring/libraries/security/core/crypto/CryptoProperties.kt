package pmeig.spring.libraries.security.core.crypto

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("spring.security.pmeig.crypto")
class CryptoProperties(
  secret: String = ""
) {
  var secret: String = secret
    get() = field.ifEmpty {
      val alphanumeric = ('a'..'z') + ('A'..'Z') + ('0'..'9')
      val random = (1..16).map { alphanumeric.random() }.joinToString("")
      println("Generated crypto secret: $random")
      secret = random
      random
    }

}
