package pmeig.spring.libraries.security.core.manager

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pmeig.spring.libraries.security.core.manager.property.TokenHeaderProperties
import pmeig.spring.libraries.security.core.model.api.ApiRequest
import pmeig.spring.libraries.security.core.model.api.ApiResponse

@Component
@ConditionalOnProperty(prefix = "spring.security.pmeig.auth.token", name = ["manager"], havingValue = "header")
class TokenHeaderManager(private val properties: TokenHeaderProperties): TokenManager {
  override fun getToken(request: ApiRequest): String? {
    return request.headers[properties.name]?.firstOrNull()?.let { return it.removePrefix(properties.prefix) }
  }

  override fun insertToken(token: String, resp: ApiResponse<*>) {
    resp.headers {
      it[properties.name] = "${properties.prefix}$token"
    }
  }

  override fun removeToken(resp: ApiResponse<*>) {
    resp.headers { it.remove(properties.name) }
  }
}