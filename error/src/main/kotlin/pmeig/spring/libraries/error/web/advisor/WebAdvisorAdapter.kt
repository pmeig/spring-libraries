package pmeig.spring.libraries.error.web.advisor

import org.springframework.web.bind.annotation.ExceptionHandler
import pmeig.spring.libraries.error.web.WebException
import pmeig.spring.libraries.logger.PmeigLoggerFactory

abstract class WebAdvisorAdapter<T> protected constructor(private val mapper: (WebException) -> T
) {

  private val log = PmeigLoggerFactory.getLogger(javaClass)


  @ExceptionHandler(WebException::class)
  fun handleException(exception: WebException): T {
    log.error("{} ({}): {}", exception.javaClass.simpleName, exception.code, exception.message, exception)
    return mapper(exception)
  }
}