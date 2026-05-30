package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository.query

import com.google.cloud.bigquery.QueryJobConfiguration
import jakarta.persistence.Entity
import pmeig.spring.libraries.jpa.core.executor.model.MethodContext
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

typealias QueryExecutor<T> = (BigQueryClient, KClass<T>, String, (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder) -> Any?

data class BigQueryAnnotationQueryExecutor(
  val query: String = "",
  val configurator: (Array<Any?>) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { { it } },
  val type: Type = Unit::class.java,
  val context: MethodContext = MethodContext()
) {

  @Suppress("UNCHECKED_CAST")
  val clazz = Class.forName(
    (if (context.collection) (type as ParameterizedType).actualTypeArguments[0] else
      if (type is ParameterizedType) type.rawType else type).typeName
  ).kotlin as KClass<Any>


  fun execute(client: BigQueryClient, args: Array<Any?>): Any? {
    return executor(client, configurator(args))
  }

  @Suppress("UNCHECKED_CAST")
  private val executor: (BigQueryClient, (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder) -> Any? =
    findExecutor()

  private fun findExecutor(): (BigQueryClient, (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder) -> Any? {
    if (context.batch) {
      return findBatchExecutor()
    }
    if (context.json) return { client, configurator -> client.tryJson(query, configurator) }
    if (context.map) {
      if (context.collection) return { client, configurator ->
        client.tryRecords(query, mapOf(), configurator)
      }
      return { client, configurator -> client.tryRecord(query, mapOf(), configurator) }
    }
    val executable = if (context.collection)
      fromType(BigQueryClient::tryEntities, BigQueryClient::tryMultiple)
    else
      fromType(BigQueryClient::tryEntity, BigQueryClient::trySingle)
    return { client, configurator -> executable(client, clazz, query, configurator) }
  }

  private fun findBatchExecutor(): (BigQueryClient, (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder) -> Any? {
    if (context.json) return { client, configurator -> client.tryBatchJson(query, configurator) }
    if (context.map) {
      if (context.collection) return { client, configurator ->
        client.tryBatchRecords(query, mapOf(), configurator)
      }
      return { client, configurator -> client.tryBatchRecord(query, mapOf(), configurator) }
    }
    val executable = if (context.collection)
      fromType(BigQueryClient::tryBatchEntities, BigQueryClient::tryBatchMultiple)
    else
      fromType(BigQueryClient::tryBatchEntity, BigQueryClient::tryBatchSingle)
    return { client, configurator -> executable(client, clazz, query, configurator) }
  }

  private fun fromType(entities: QueryExecutor<Any>, other: QueryExecutor<Any>): QueryExecutor<Any> {
    return if (clazz.findAnnotations(Entity::class).isNotEmpty()) {
      entities
    } else
      other
  }
}
