package pmeig.spring.libraries.jpa.core.cache

import org.springframework.util.ClassUtils
import tools.jackson.core.type.TypeReference
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

interface DataCacheNames {
  companion object {
    const val INVOKER = "data-invoker"
    const val DATA_CONTEXT = "data-context"
    const val METADATA = "data-metadata"
    const val COLUMNS = "data-columns"
    const val AUDITING = "data-auditing"

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : Any> useCache(
      cacheManager: DataCacheManager, name: String,
      key: String,
      target: TypeReference<T>, compute: () -> T
    ): T {
      val type = target.type
      val clazz = Class.forName(if (type is ParameterizedType) {
        type.rawType.typeName
      } else type.typeName)
      return byCache(cacheManager, name, key, clazz as Class<Any>, compute) as T
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : Any> useCache(
      cacheManager: DataCacheManager, name: String,
      key: String, target: KClass<T>, compute: () -> T
    ): T {
      return byCache(cacheManager, name, key, target.java as Class<Any>, compute) as T
    }

    private fun byCache(cacheManager: DataCacheManager, name: String,
                          key: String, target: Class<Any>, compute: () -> Any): Any {
      var cached = cacheManager.getCache(name)?.get(key, target)
      if (cached == null) {
        cached = compute()
        cacheManager.getCache(name)?.put(key, cached)
      }
      return cached
    }

    @JvmStatic
    fun generateKeyFromMethod(method: Method, prefix: String = "") = "${if(prefix.isEmpty()) "" else "$prefix::"}${method.name}::" +
            "${method.parameterTypes.joinToString(".") {  it.typeName + "@" + it.name }}::${ClassUtils.getUserClass(method.declaringClass).typeName}"

  }
}