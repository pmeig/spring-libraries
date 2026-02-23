package pmeig.spring.libraries.security.core.manager

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pmeig.spring.libraries.security.core.models.api.ApiRequest
import pmeig.spring.libraries.security.core.models.api.ApiResponse

@Component
@ConditionalOnProperty(prefix = "spring.plugins.pmeig.security.auth.token", name = ["manager"], havingValue = "body")
class TokenBodyManager: TokenManager {
  override fun getToken(request: ApiRequest): String? = request.body

  override fun insertToken(token: String, resp: ApiResponse<*>) {
    resp.body(token)
  }

  override fun removeToken(resp: ApiResponse<*>) {
  }
}