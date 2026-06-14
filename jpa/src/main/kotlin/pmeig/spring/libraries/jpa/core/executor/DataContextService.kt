package pmeig.spring.libraries.jpa.core.executor

import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Service
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.executor.model.MethodContext
import pmeig.spring.libraries.jpa.core.executor.model.ParameterContext
import pmeig.spring.libraries.jpa.core.executor.model.QueryContext
import pmeig.spring.libraries.jpa.core.executor.model.jpa.JpaMethodNameContext
import pmeig.spring.libraries.jpa.core.executor.model.jpa.MethodSpecificationFactory
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.regex.Pattern

private val POSITIONAL_PARAMETER_MARKER = Pattern.compile(" \\?[0-9]*")
private val POSITIONAL_PARAMETER_FIX_MARKER = Pattern.compile("[0-9]*$").asPredicate()

private val METHOD_NAME_PREFIX = Pattern.compile("^findBy.*").asPredicate()
private val METHOD_COLLECTION_NAME_PREFIX = Pattern.compile("^findAllBy.*").asPredicate()


@Service
class DataContextService(
  private val cacheManager: DataCacheManager
) {

  companion object {
    const val POSITIONAL_PARAMETER_NAME = "paramPositional"
  }

  @Suppress("UNCHECKED_CAST")
  fun context(method: Method, returnType: Type): MethodContext =
    DataCacheNames.useCache(cacheManager, DataCacheNames.DATA_CONTEXT, DataCacheNames.generateKeyFromMethod(method, "parameters"),
      MethodContext::class) {
      val isCollection = checkIsCollection(returnType)
      MethodContext(constructParameterContexts(method),
        returnType.typeName.startsWith(String::class.javaObjectType.typeName)
                && method.name.endsWith("toJson", ignoreCase = true),
        checkIsMap(returnType, isCollection),
        method.name.startsWith("batch", ignoreCase = true),
        isCollection
        )
    }

  fun query(method: Method, returnType: Type) = DataCacheNames.useCache(cacheManager, DataCacheNames.DATA_CONTEXT,
    DataCacheNames.generateKeyFromMethod(method, "query"), QueryContext::class) {
    val query = AnnotatedElementUtils.findMergedAnnotation(method, Query::class.java) ?: return@useCache QueryContext(
      ""
    )
    context(method, returnType).let {
      QueryContext(constructSql(query, it.parameters), it)
    }
  }.let {
    if (it.sql.isEmpty()) null else it
  }

  fun jpa(method: Method) = DataCacheNames.useCache(cacheManager, DataCacheNames.DATA_CONTEXT,
    DataCacheNames.generateKeyFromMethod(method, "jpa"), JpaMethodNameContext::class) {
    generateSpecificationFactory(method)?.let {
      JpaMethodNameContext(it.first, it.second, true)
    } ?: JpaMethodNameContext()
  }

  private fun constructSql(
    query: Query,
    parameters: List<ParameterContext>
  ): String {
    val sql = query.value
    return if (parameters.any { it.name.startsWith(POSITIONAL_PARAMETER_NAME) }) {
      var positional = 0
      POSITIONAL_PARAMETER_MARKER.matcher(sql).replaceAll {
        val name = it.group()
        " :" + if (POSITIONAL_PARAMETER_FIX_MARKER.test(name)) {
          POSITIONAL_PARAMETER_NAME + name.substring(2)
        } else POSITIONAL_PARAMETER_NAME + positional++
      }
    } else sql
  }

  private fun constructParameterContexts(method: Method) = method.parameters.mapIndexed { index, parameter ->
    ParameterContext(
      AnnotatedElementUtils.getMergedAnnotation(parameter, Param::class.java)
      ?.let {
        it.value.ifEmpty { null } ?: parameter.name
      } ?: "$POSITIONAL_PARAMETER_NAME$index", parameter.parameterizedType, index)
  }

  @Suppress("UNCHECKED_CAST")
  private fun generateSpecificationFactory(method: Method): Pair<(Array<Any?>) -> Specification<Any>, (Collection<Any?>) -> Any?>? {
    if (METHOD_NAME_PREFIX.test(method.name)) {
      return Pair(toSpecificationFactory(method.name.substringAfter("By"))) {
        it.firstOrNull()
      }
    }
    if (METHOD_COLLECTION_NAME_PREFIX.test(method.name)) {
      return Pair(toSpecificationFactory(method.name.substringAfter("By"))) { it }
    }
    return null
  }

  @Suppress("UNCHECKED_CAST")
  private fun toSpecificationFactory(
    methodName: String
  ): (Array<Any?>) -> Specification<Any> {
    val factories = methodName.split("And", ignoreCase =  true).map {
      MethodSpecificationFactory(it)
    }
    return {
      val arguments = it.toMutableList()
      factories.map { factory -> factory.factory(arguments) }.reduce { acc, specification ->
        acc.and(specification)
      }
    }
  }

  private fun checkIsCollection(returnType: Type): Boolean {
    if (returnType !is ParameterizedType) return false
    val rawType = returnType.rawType as Class<*>
    return Collection::class.java.isAssignableFrom(rawType)
  }

  private fun checkIsMap(returnType: Type, isCollection: Boolean = false): Boolean {
    if (isCollection) {
      val type = (returnType as ParameterizedType).actualTypeArguments[0]
      return checkIsMap(type)
    }
    if (returnType !is ParameterizedType) return false
    val rawType = (returnType.rawType as Class<*>)
    return Map::class.java.isAssignableFrom(rawType)
  }

}