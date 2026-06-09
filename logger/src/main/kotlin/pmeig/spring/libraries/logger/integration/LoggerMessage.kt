package pmeig.spring.libraries.logger.integration

import org.slf4j.Marker
import org.slf4j.event.Level
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.security.MessageDigest

private val SHA_ALGORITHM = MessageDigest.getInstance("SHA-512")

class LoggerMessage(
  val name: String,
  val message: String,
  val level: Level,
  val marker: Marker? = null,
  val cause: Throwable? = null,
  vararg val args: Any?
) : Message<LoggerMessage>, Serializable {

  companion object {
    @Suppress("unused")
    private const val serialVersionUID: Long = 948391655037021860L
  }

  override fun getPayload() = this

  override fun getHeaders(): MessageHeaders {
    val arrayOutputStream = ByteArrayOutputStream()
    val writer = ObjectOutputStream(arrayOutputStream)
    writer.writeObject(this)
    writer.flush()
    return MessageHeaders(mapOf(Pair("name", name), Pair("level", level), Pair("exception", cause?.let { true } ?: false), Pair(
      "id",
      encodeHex(SHA_ALGORITHM.digest(this.message.toByteArray()))
    ))).apply {
      writer.close()
      arrayOutputStream.close()
    }
  }

  private fun encodeHex(bytes: ByteArray): String {
    val hexString = StringBuilder()
    for (byte in bytes) {
      val hex = Integer.toHexString(0xff and byte.toInt())
      if (hex.length == 1) hexString.append('0')
      hexString.append(hex)
    }
    return hexString.toString()
  }


}
