package pmeig.spring.libraries.jpa.data.bigquery.v2

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.v2.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.v2.mapper.factory.BigQueryMapperFactory
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.reflect.KClass

abstract class BigQueryMultiClient(
  private val mapperFactory: BigQueryMapperFactory,
  private val entityAnnotationReader: EntityAnnotationReader
) {
  private val cacheMappers = mutableMapOf<KClass<*>, Map<String, BigQueryMapper<*>>>()
  abstract fun query(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult

  abstract fun tryQuery(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult?

  fun multiRecord(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>> {
    return recorded {
      query(sql, configurator)
    } ?: emptyList()
  }

  fun tryMultiRecord(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>>? {
    return recorded {
      tryQuery(sql, configurator)
    } ?: emptyList()
  }

  protected fun <T : Any> toEntity(entity: KClass<T>, executor: () -> TableResult?) = exec(executor) { tableResult ->
    val metadata = cacheMappers.getOrPut(entity) {
      val metadata = entityAnnotationReader.metadata(entity)
      val schema = tableResult.schema
      metadata.columns.entries.map {
        it.key to mapperFactory.toMetadataFactory(it.value)
      }
    }
    it.iterateAll().map { row ->
    }
  }

  protected fun recorded(executor: () -> TableResult?) = exec(
    executor
  ) { tableResult ->
    val mappers = mapperFactory.fromSchema(tableResult.schema)
    tableResult.iterateAll().map {
      mappers.entries.associate { (name, mapper) ->
        name to mapper.map(it.get(name))
      }
    }
  }

  protected fun <T> exec(executor: () -> TableResult?, transform: (TableResult) -> T?) = executor()?.let {
    transform(it)
  }
}