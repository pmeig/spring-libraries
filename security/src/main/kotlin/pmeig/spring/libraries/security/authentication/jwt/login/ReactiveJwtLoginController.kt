package pmeig.spring.libraries.security.authentication.jwt.login

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.ServerResponse
import pmeig.spring.libraries.security.authentication.jwt.JwtService
import pmeig.spring.libraries.security.authentication.jwt.model.RequestUser
import pmeig.spring.libraries.security.core.manager.TokenManager
import pmeig.spring.libraries.security.core.models.api.ReactiveAdapter
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/login")
@ConditionalOnWebApplication(type = REACTIVE)
@ConditionalOnProperty(prefix = "spring.plugins.pmeig.security.auth", name = ["login"], havingValue = "true")
class ReactiveJwtLoginController(private val jwtService: JwtService, private val tokenManager: TokenManager) {
  @PostMapping
  fun login(@RequestBody user: RequestUser): Mono<ServerResponse> {
    val apiResponse = ReactiveAdapter()
    tokenManager.insertToken(jwtService.createToken(user) ?: "", apiResponse)
    return apiResponse.toResponseEntity()
  }

  @PutMapping
  fun disconnect(request: ServerHttpRequest): Mono<ServerResponse> {
    val apiResponse = ReactiveAdapter(request)
    tokenManager.removeToken(apiResponse)
    return apiResponse.toResponseEntity()
  }
}