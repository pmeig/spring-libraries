package pmeig.spring.libraries.logger.integration.configurer.log.mdc

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder


@Configuration
@ConditionalOnClass(SecurityContextHolder::class)
@ConditionalOnBooleanProperty(prefix = "spring.logger.pmeig.mdc", name = ["user"], havingValue = true)
class SpringUserMDCConfigurer: MDCConfigurer {
  override fun configure(): Map<String, String> {
    val mdc = mutableMapOf<String, String>()
    SecurityContextHolder.getContext().authentication?.let {
      it.principal?.let { principal ->
        mdc.putAll(extractFields(principal))
      }
      mdc["user.authorities"] = "[" + it.authorities.joinToString(",") { authority -> authority.authority ?: "" } + "]"
      mdc.putAll(extractValue(it.credentials, "security.credentials"))
      mdc.putAll(extractValue(it.details, "security.details"))
      mdc["security.authentication.name"] = it.name ?: ""
    }
    return mdc
  }

  private fun extractFields(item: Any, prefix: String = "user"): List<Pair<String, String>> {
    val clazz = item.javaClass
    return clazz.declaredFields.flatMap { field ->
      field.isAccessible = true
      val parent = "$prefix.${field.name}"
      when (val value = field[item]) {
        is Collection<*> -> iterableToPairs(value.iterator(), parent)
        is Array<*> -> iterableToPairs(value.iterator(), parent)
        is String, is Number, is Boolean -> listOf(Pair(parent, value.toString()))
        else -> extractFields(value, parent)
      }
    }
  }

  private fun extractValue(value: Any?, prefix: String): List<Pair<String, String>> = when (value) {
    null -> listOf()
    is Collection<*> -> iterableToPairs(value.iterator(), prefix)
    is Array<*> -> iterableToPairs(value.iterator(), prefix)
    is String, is Number, is Boolean -> listOf(Pair(prefix, value.toString()))
    else -> extractFields(value, prefix)
  }

  private fun iterableToPairs(iterator: Iterator<*>, prefix: String): List<Pair<String, String>> {
    var list = "["
    val pairs = mutableListOf<Pair<String, String>>()
    val iteratorIndexed = iterator.withIndex()
    while (iteratorIndexed.hasNext()) {
      val (index, value) = iteratorIndexed.next()
      pairs.addAll(extractValue(value, "$prefix.[$index]"))
      list += pairs.lastOrNull()?.second ?: ""
      if (iteratorIndexed.hasNext()) {
        list += ","
      }
    }
    return pairs.apply { add(Pair(prefix, "$list]")) }
  }
}