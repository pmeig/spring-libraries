package pmeig.spring.libraries.error.core.exception

import java.io.Serial

open class TechnicalException(
  val code: String,
  message: String? = null,
  cause: Throwable? = null,
  enableSuppression: Boolean = false,
  writableStackTrace: Boolean = true
): Exception(message, cause, enableSuppression, writableStackTrace) {
  companion object {
    @Serial
    private const val serialVersionUID: Long = -7549894102580710339L
  }
}