package pmeig.spring.libraries.security.core.crypto

import org.springframework.stereotype.Service
import kotlin.io.encoding.Base64

@Service
class CryptoService(private val cryptoProperties: CryptoProperties) {
  fun encrypt(value: String?): String {
    if (value.isNullOrEmpty()) return ""
    val secret = cryptoProperties.getOrGenerateSecret().toByteArray()
    var index = 0
    val bytes = value.toByteArray()
    val encrypted = bytes.map {
      (it + secret[index++ % secret.size]).toByte()
    }.toByteArray()
    return Base64.encode(encrypted)
  }

  fun decrypt(value: String?): String {
    if (value.isNullOrEmpty()) return ""
    val secret = cryptoProperties.getOrGenerateSecret().toByteArray()
    val decoded = Base64.decode(value)
    var index = 0
    return decoded.map {
      (it - secret[index++ % secret.size]).toByte()
    }.toByteArray().toString(Charsets.UTF_8)
  }

}