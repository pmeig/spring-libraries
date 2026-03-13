package pmeig.spring.libraries.logger.integration.configurer.log.argument

import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder

@Configuration
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class LoggerUserProvider: LoggerArgumentProvider {
  override fun provide(argument: String): String? {
    return if (listOf("user.", "security.").none { argument.startsWith(it) }) {
      null
    } else {
      SecurityContextHolder.getContext().authentication?.let {
        var target: Any? = if(argument.startsWith("user.")) it.principal!! else it
        argument.split(".").stream().skip(1).takeWhile { null != target }.forEach { arg ->
          if (arg == "authorities") "[" + it.authorities.joinToString(",") { item -> item.authority.toString() } + "]"
          else {
            try {
              target = target?.javaClass?.getField(arg)?.apply { isAccessible = true }?.get(target)
            } catch (_: NoSuchFieldException) {
              target = null
            }
          }
        }
        target
      }?.toString()
    }
  }
}