package pmeig.spring.libraries.logger.integration

import org.slf4j.Marker
import org.slf4j.event.Level
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.util.DigestUtils
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable

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
      DigestUtils.md5DigestAsHex(arrayOutputStream.toByteArray())
    ))).apply {
      writer.close()
      arrayOutputStream.close()
    }
  }


}
