package pmeig.spring.libraries.logger.correlation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter
import pmeig.spring.libraries.logger.PmeigLoggerFactory
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.correlation.correlationId
import pmeig.spring.libraries.logger.correlation.insertCorrelationId
import java.util.UUID


@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OncePerRequestFilter::class)
class CorrelationConfiguration(
  private val correlationProperties: CorrelationProperties
) : OncePerRequestFilter() {
  private val log = PmeigLoggerFactory.getLogger(CorrelationConfiguration::class.java)

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    val correlationId = request.getHeader(correlationProperties.header)?.let {
      UUID.fromString(it)
    } ?: correlationId

    request.setAttribute(correlationProperties.header, correlationId)
    insertCorrelationId(correlationId)
    log.info("Request with correlation id {} received", correlationId)
    filterChain.doFilter(request, response)
  }
}

