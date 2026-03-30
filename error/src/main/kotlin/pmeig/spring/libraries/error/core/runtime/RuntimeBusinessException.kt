package pmeig.spring.libraries.error.core.runtime

import java.io.Serial

@Suppress("unused")
open class RuntimeBusinessException(
  code: String,
  val business: Any,
  message: String? = null,
  cause: Throwable? = null,
  enableSuppression: Boolean = false,
  writableStackTrace: Boolean = true
) : RuntimeTechnicalException(code, message, cause, enableSuppression, writableStackTrace) {
  companion object {
    @Serial
    private const val serialVersionUID: Long = 8019775523995546958L
  }


  constructor(
    business: Any, technicalCause: RuntimeTechnicalException, code: String = technicalCause.code,
    enableSuppression: Boolean = false,
    writableStackTrace: Boolean = true
  ) : this(code, business, technicalCause.message, technicalCause.cause,
    enableSuppression, writableStackTrace)

}