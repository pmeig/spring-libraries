package pmeig.spring.libraries.security.core.crypto

import org.springframework.security.crypto.codec.Hex
import org.springframework.security.crypto.encrypt.AesBytesEncryptor
import org.springframework.stereotype.Service

@Service
class CryptoService(
  cryptoProperties: CryptoProperties,
) {

  private val encryptor = createAESEncryptor(cryptoProperties)

  fun encrypt(value: String?): String {
    if (value.isNullOrEmpty()) return ""
    return Hex.encode(encryptor.encrypt(value.toByteArray())).concatToString()
  }

  fun decrypt(value: String?): String {
    if (value.isNullOrEmpty()) return ""
    return encryptor.decrypt(Hex.decode(value)).decodeToString()
  }

  private fun createAESEncryptor(cryptoProperties: CryptoProperties): AesBytesEncryptor = AesBytesEncryptor(
    cryptoProperties.secret,
    cryptoProperties.salt
  )

}