package pmeig.spring.libraries.jpa.data.bigquery.client

import com.google.cloud.bigquery.QueryJobConfiguration
import org.springframework.cache.CacheManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQuerySqlMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import kotlin.reflect.KClass

@Suppress("unused")
abstract class BigQueryMultiBatchClient(
  mapperFactory: BigQueryMapperFactory,
  entityAnnotationReader: EntityAnnotationReader,
  sqlMapper: BigQuerySqlMapper,
  cacheManager: CacheManager
) : BigQueryMultiClient(mapperFactory, entityAnnotationReader, sqlMapper, cacheManager) {

  @JvmOverloads
  fun batchRecords(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>> =
    records(sql, metadataFactory, toBatch(configurator))

  @JvmOverloads
  fun tryBatchRecords(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>> =
    tryRecords(sql, metadataFactory, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> batchEntities(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> batchEntities(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> tryBatchEntities(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    tryEntities(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> tryBatchEntities(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    tryEntities(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> batchEntities(
    entityRef: KClass<T>,
    pageable: Pageable,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Page<T> =
    entities(entityRef, pageable, sql, toBatch(configurator))

  protected fun toBatch(
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = {
    configurator(it.setPriority(QueryJobConfiguration.Priority.BATCH))
  }
}