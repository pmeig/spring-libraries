package pmeig.spring.libraries.security.core.manager.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import java.time.Duration

@ConfigurationProperties("spring.plugins.pmeig.security.auth.token.cookie")
class TokenCookieProperties(
  var name: String = "",
  var path: String = "/",
  exp: Duration? = null,
  var secure: Boolean = true,
  var httpOnly: Boolean = true,
  var domain: String = "localhost:8080",
  var sameSite: String = "Strict",
  var partitioned: Boolean = false
): EnvironmentAware, TokenExpirationProperty(exp) {

  lateinit var tokenName: String
    private set

  override fun setEnvironment(environment: Environment) {
    tokenName = name.ifEmpty {
      "${environment.getProperty("spring.application.name") ?: "myproject"}-token"
    }
  }
}
