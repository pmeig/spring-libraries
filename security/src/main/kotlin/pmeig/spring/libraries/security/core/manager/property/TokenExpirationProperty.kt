package pmeig.spring.libraries.security.core.manager.property

import pmeig.spring.libraries.security.expiration
import java.time.Duration

abstract class TokenExpirationProperty(
  exp: Duration? = null
) {
  var exp: Duration? = exp
    get() = field ?: expiration
}
