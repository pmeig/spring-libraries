package pmeig.spring.libraries.security.core.crypto

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.codec.Hex

@Configuration
@ConfigurationProperties("spring.security.pmeig.crypto")
class CryptoProperties(
  secret: String = "",
  salt: String = ""
) {
  var salt: String = salt
    get() = field.ifEmpty {
      val random = Hex.encode(generateSecret().toByteArray()).concatToString()
      println("Generated crypto salt: $random")
      field = random
      random
    }
  var secret: String = secret
    get() = field.ifEmpty {
      val alphanumeric = ('a'..'z') + ('A'..'Z') + ('0'..'9')
      val random = (1..16).map { alphanumeric.random() }.joinToString("")
      println("Generated crypto secret: $random")
      field = random
      random
    }

  private fun generateSecret(): String {
    val alphanumeric = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..16).map { alphanumeric.random() }.joinToString("")
  }

}
