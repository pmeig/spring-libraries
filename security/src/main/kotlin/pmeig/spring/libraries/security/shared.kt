package pmeig.spring.libraries.security

import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import java.time.Duration
import java.util.LinkedList

internal var expiration: Duration = Duration.ofHours(4)

abstract class ExpirationUpdater(var type: String = ""): EnvironmentAware {
  private var environment: Environment? = null
  open var exp: Duration = Duration.ofHours(4)
    set(value) {
      field = value
      actualizeExpiration(value)
    }
  override fun setEnvironment(environment: Environment) {
    this.environment = environment
    actualizeExpiration(this.exp)
  }

  private fun actualizeExpiration(exp: Duration) {
    val authType = (environment?.getProperty("spring.security.pmeig.auth.type", LinkedList::class.java)
      ?: listOf("cookie")).map { it.toString().lowercase() }
    if (authType.any{ it == type }) expiration = exp
  }
}