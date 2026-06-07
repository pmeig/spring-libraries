package pmeig.spring.libraries.jpa.core.executor

import org.springframework.cache.get
import org.springframework.cglib.proxy.InvocationHandler
import pmeig.spring.libraries.jpa.core.auditing.AuditingManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.converter.result.DataConverter
import tools.jackson.core.type.TypeReference
import java.lang.reflect.Method
import java.lang.reflect.Type

class JpaRepositoryHandler(
  private val methodInvokers: List<JpaMethodInvoker>,
  private val converters: List<DataConverter>,
  private val auditingManager: AuditingManager,
  private val cacheManager: DataCacheManager
): InvocationHandler {
  override operator fun invoke(proxy: Any, method: Method, args: Array<Any?>): Any? {
    val cacheName = DataCacheNames.generateKeyFromMethod(method)
    val cache = cacheManager.getCache(DataCacheNames.INVOKER)!!
    var invoker = cache.get<JpaMethodInvoker>(DataCacheNames.generateKeyFromMethod(method))
    val (returnType, converter) = determineConverter(method)
    auditEntity(method, args)
    val jpaResult = if (invoker == null) {
      var invokerResult = JpaMethodInvokerResult()
      val iterator = methodInvokers.iterator()
      while (iterator.hasNext() && !invokerResult.executed) {
        invoker = iterator.next()
        invokerResult = invoker.invokeMethod(method, returnType, args)
      }
      cache.put(cacheName, invoker)
      invokerResult
    } else invoker.invokeMethod(method, returnType, args)
    return converter.convert(jpaResult.result)
  }

  private fun determineConverter(method: Method) = DataCacheNames.useCache(
    cacheManager,
    DataCacheNames.INVOKER,
    "converter::" + DataCacheNames.generateKeyFromMethod(method),
    object : TypeReference<Pair<Type, DataConverter>>(){}
  )  {
    var previousType = method.genericReturnType
    var newType: Type?

    var iterator = converters.iterator()
    var converter = DataConverter { _, returnType -> returnType }

    while (iterator.hasNext()) {
      val currentConverter = iterator.next()
      newType = currentConverter.typeChange(method, previousType)
      if (null != newType) {
        previousType = newType
        converter = converter.andThen(currentConverter)
        iterator = converters.iterator()
      }
    }
    Pair(previousType, converter)
  }

  private fun auditEntity(method: Method, args: Array<Any?>) {
    val key = "audit::" + DataCacheNames.generateKeyFromMethod(method)
    val cache = cacheManager.getCache(DataCacheNames.INVOKER)!!
    var auditing = cache.get<Int>(key)
    if (null == auditing) {
      auditing = -1
      args.find { param ->
        auditing++
        param?.let { auditingManager.applyAuditing(it) } == true
      }
      if (auditing!! >= args.size) {
        auditing = -1
      }
      cache.put(key, auditing)
      return
    }
    if (auditing > -1) {
      auditingManager.applyAuditing(args[auditing]!!)
    }
  }
}