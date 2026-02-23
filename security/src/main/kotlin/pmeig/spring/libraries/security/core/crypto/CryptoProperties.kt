package pmeig.spring.libraries.security.core.crypto

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.plugins.pmeig.security.crypto")
data class CryptoProperties(
  var secret: String = ""
) {
  fun getOrGenerateSecret(): String = secret.ifEmpty {
    val random = (1..16).map { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("")
    println("Generated crypto secret: $random")
    secret = random
    random
  }
}
