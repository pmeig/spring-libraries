package pmeig.spring.libraries.jpa.data.bigquery.client

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQuerySqlMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import kotlin.reflect.KClass


abstract class BigQueryClientSimple(
  entityAnnotationReader: EntityAnnotationReader,
  bigQuerySqlMapper: BigQuerySqlMapper,
  cacheManager: DataCacheManager,
  mapperFactory: BigQueryMapperFactory,
) : BigQueryMultiBatchClient(mapperFactory, entityAnnotationReader, bigQuerySqlMapper, cacheManager) {

  fun <T: Any> single(target: KClass<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }) =
    multiple(target, sql, configurator).firstOrNull()

  fun <T: Any> trySingle(target: KClass<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }) =
    tryMultiple(target, sql, configurator).firstOrNull()


  @JvmOverloads
  fun <T : Any> tryEntity(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    tryEntities(entityRef, sql, configurator).firstOrNull()

  fun <T : Any> entity(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef, sql, configurator).firstOrNull()

  @JvmOverloads
  fun tryJson(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): String? {
    return toJson(sql) { tryQuery(it, configurator) }
  }

  @JvmOverloads
  fun json(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): String? {
    return toJson(sql) { query(it, configurator) }
  }

  fun tryRecord(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Map<String, Any?> =
    tryRecords(sql, metadataFactory, configurator).firstOrNull() ?: emptyMap()

  @JvmOverloads
  fun record(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Map<String, Any?> = records(sql, metadataFactory, configurator).firstOrNull() ?: emptyMap()


  private fun toJson(sql: String, executor: (sql: String) -> TableResult?): String? {
    val toJsonQuery = "WITH request AS ($sql) " +
            "SELECT to_json_string(request) AS json FROM request request"
    return exec({
      executor(toJsonQuery)
    }) {
      it.iterateAll().firstOrNull()?.get("json")?.stringValue
    }
  }

}