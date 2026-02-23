package pmeig.spring.libraries.security.core.manager

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import pmeig.spring.libraries.security.core.manager.property.TokenCookieProperties
import pmeig.spring.libraries.security.core.models.api.ApiRequest
import pmeig.spring.libraries.security.core.models.api.ApiResponse
import java.time.Duration

@Component
@ConditionalOnProperty(prefix = "spring.plugins.pmeig.security.auth.token", name = ["manager"], havingValue = "cookie", matchIfMissing = true)
class TokenCookieManager(private val properties: TokenCookieProperties): TokenManager {

  override fun getToken(request: ApiRequest): String? = request.cookies.find { it.name == properties.name }?.value

  override fun insertToken(token: String, resp: ApiResponse<*>) {
    resp.cookies { ResponseCookie.from(properties.name, token)
      .path(properties.path)
      .secure(properties.secure)
      .httpOnly(properties.httpOnly)
      .domain(properties.domain)
      .partitioned(properties.partitioned)
      .sameSite(properties.sameSite)
      .maxAge(properties.exp ?: Duration.ofHours(4))
      .build()
    }
  }

  override fun removeToken(resp: ApiResponse<*>) {
    resp.cookies { ResponseCookie.from(properties.name, "").maxAge(0).build() }
  }
}