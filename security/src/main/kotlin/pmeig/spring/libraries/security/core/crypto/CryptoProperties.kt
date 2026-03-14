package pmeig.spring.libraries.security.core.crypto

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.plugins.pmeig.security.crypto")
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
