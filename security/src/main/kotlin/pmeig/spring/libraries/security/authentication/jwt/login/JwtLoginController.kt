package pmeig.spring.libraries.security.authentication.jwt.login

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.http.ResponseEntity
import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pmeig.spring.libraries.security.authentication.jwt.JwtService
import pmeig.spring.libraries.security.authentication.jwt.model.RequestUser
import pmeig.spring.libraries.security.core.manager.TokenManager
import pmeig.spring.libraries.security.core.models.api.ApiResponse
import pmeig.spring.libraries.security.core.models.api.ServletAdapter

@RestController
@RequestMapping("/login")
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnProperty(prefix = "spring.plugins.pmeig.security.auth", name = ["login"], havingValue = "true")
class JwtLoginController(private val jwtService: JwtService,
                         private val tokenManager: TokenManager) {
  @PostMapping
  fun login(@RequestBody user: RequestUser): ResponseEntity<*> {
    val apiResponse: ApiResponse<ResponseEntity<*>> = ServletAdapter()
    tokenManager.insertToken(jwtService.createToken(user) ?: "", apiResponse)
    return apiResponse.toResponseEntity()
  }

  @PutMapping
  fun disconnect(request: ServerHttpRequest): ResponseEntity<*> {
    val apiResponse = ServletAdapter(request)
    tokenManager.removeToken(apiResponse)
    return apiResponse.toResponseEntity()
  }
}