package pmeig.spring.libraries.error.core.exception

import java.io.Serial

@Suppress("unused")
open class BusinessException(
  code: String,
  val business: Any,
  message: String? = null,
  cause: Throwable? = null,
  enableSuppression: Boolean = false,
  writableStackTrace: Boolean = true
) : TechnicalException(code, message, cause, enableSuppression, writableStackTrace) {

  companion object {
    @Serial
    private const val serialVersionUID: Long = 8019775523995546958L
  }

  constructor(
    business: Any, technicalCause: TechnicalException, code: String = technicalCause.code,
    enableSuppression: Boolean = false,
    writableStackTrace: Boolean = true
  ) : this(code, business, technicalCause.message, technicalCause.cause,
    enableSuppression, writableStackTrace)

}