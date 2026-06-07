package pmeig.spring.libraries.security.core.manager

import pmeig.spring.libraries.security.core.model.api.ApiRequest
import pmeig.spring.libraries.security.core.model.api.ApiResponse

interface TokenManager {
  fun getToken(request: ApiRequest): String?
  fun insertToken(token: String, resp: ApiResponse<*>)
  fun removeToken(resp: ApiResponse<*>)
}