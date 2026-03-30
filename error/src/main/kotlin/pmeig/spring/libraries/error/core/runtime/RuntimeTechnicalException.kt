package pmeig.spring.libraries.error.core.runtime

import java.io.Serial

open class RuntimeTechnicalException(
  val code: String,
  message: String? = null,
  cause: Throwable? = null,
  enableSuppression: Boolean = false,
  writableStackTrace: Boolean = true
): RuntimeException(message, cause, enableSuppression, writableStackTrace) {
  companion object {
    @Serial
    private const val serialVersionUID: Long = -7549894102580710339L
  }
}