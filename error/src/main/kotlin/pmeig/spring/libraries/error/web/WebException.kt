package pmeig.spring.libraries.error.web

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.function.ServerResponse
import pmeig.spring.libraries.error.core.exception.TechnicalException
import pmeig.spring.libraries.error.core.runtime.RuntimeTechnicalException
import java.io.Serial

open class WebException(
  code: String,
  message: String? = null,
  val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
  val body: Any? = null,
  val headers: HttpHeaders = HttpHeaders()
): RuntimeTechnicalException(code, message, null, false, true) {
  companion object {
    @Serial
    private const val serialVersionUID: Long = -8929436325005189148L
    const val EXCEPTION_CODE_HEADER = "WEB_EXCEPTION"
  }

  @Suppress("unused")
  constructor(exception: TechnicalException, status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
              body: Any? = null, headers: HttpHeaders = HttpHeaders()): this(exception.code, exception.message, status, body, headers)

  init {
    headers[EXCEPTION_CODE_HEADER] = "${javaClass.simpleName}: $code"
  }

  open fun toResponse() = ResponseEntity.status(status).headers(headers).body(body)
  open fun toReactiveResponse() = ServerResponse.status(status).headers{
    it.addAll(headers)
  }.apply { body?.let { body(it)} }.build()
}