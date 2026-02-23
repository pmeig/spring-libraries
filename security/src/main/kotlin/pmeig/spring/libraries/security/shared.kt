package pmeig.spring.libraries.security

import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import java.time.Duration

internal var expiration: Duration = Duration.ofHours(4)

abstract class ExpirationUpdater(var type: String = ""): EnvironmentAware {
  private var environment: Environment? = null
  open var exp: Duration = Duration.ofHours(4)
    set(value) {
      field = value
      val authType = (environment?.getProperty("spring.plugins.pmeig.security.auth.type") ?: "cookie")
      if (type == authType) expiration = value
    }
  override fun setEnvironment(environment: Environment) {
    this.environment = environment
    environment.getProperty("spring.plugins.pmeig.security.auth.type")?.let {
      if (type == it) expiration = exp
    }
  }
}