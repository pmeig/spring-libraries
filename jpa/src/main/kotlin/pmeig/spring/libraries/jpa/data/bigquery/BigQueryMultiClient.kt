package pmeig.spring.libraries.jpa.data.bigquery

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.TableResult
import org.springframework.cache.CacheManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQuerySqlMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import kotlin.reflect.KClass

@Suppress("unused")
abstract class BigQueryMultiClient(
  private val mapperFactory: BigQueryMapperFactory,
  private val entityAnnotationReader: EntityAnnotationReader,
  private val sqlMapper: BigQuerySqlMapper,
  protected val cacheManager: CacheManager
) {
  private val cacheMetadata = mutableMapOf<KClass<*>, DataMetadata>()
  abstract fun query(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult

  abstract fun tryQuery(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult?

  @JvmOverloads
  fun records(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>> {
    return recorded(metadataFactory) {
      query(sql, configurator)
    } ?: emptyList()
  }

  @JvmOverloads
  fun tryRecords(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>> {
    return recorded(metadataFactory) {
      tryQuery(sql, configurator)
    } ?: emptyList()
  }

  fun <T : Any> tryEntities(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef.kotlin, sql, configurator)

  fun <T : Any> tryEntities(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    toEntity(entityRef) { tryQuery(sql, configurator) } ?: emptyList()

  fun <T : Any> entities(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef.kotlin, sql, configurator)

  fun <T : Any> entities(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    toEntity(entityRef) { query(sql, configurator) } ?: emptyList()

  fun <T : Any> entities(
    entityRef: KClass<T>,
    pageable: Pageable,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Page<T> {
    val metadata = getMetadata(entityRef)
    val tableResult = query(sqlMapper.pageQuery(sql, pageable, metadata))
    val result = tableResult.iterateAll().first()
    val items = result.get("items").repeatedValue!!
    val total = result.get("total").longValue
    val entities = toEntity(entityRef, metadata) {
      TableResult.newBuilder()
        .setSchema(Schema.of(tableResult.schema!!.fields.get("items").subFields))
        .setPageNoSchema(BigQueryPage(items))
        .build()
    }!!
    return PageImpl(entities, pageable, total)
  }


  @Suppress("UNCHECKED_CAST")
  private fun <T : Any> toEntity(entity: KClass<T>, metadata: DataMetadata = getMetadata(entity), executor: () -> TableResult?) = exec(executor) { tableResult ->
    tableResult.iterateAll().map {
      val newEntity = metadata.createEntity() as T
      val mappers = DataCacheNames.useCache(cacheManager, DataCacheNames.MAPPER_FIELDS,
        entity.qualifiedName!! + "_" + metadata.columns.keys.joinToString("-"), Map::class.java) {
        createEntityFieldMappers(entity, tableResult, metadata)
      } as Map<String, BigQueryFieldMapper<*>>
      mappers.forEach { (fieldName, mapper) ->
        val fieldValue = it.get(fieldName)
        mapper.map(newEntity, fieldValue)
      }
      newEntity
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T : Any> createEntityFieldMappers(
    entity: KClass<T>,
    tableResult: TableResult,
    metadata: DataMetadata
  ): Map<String, BigQueryFieldMapper<*>> {
    val schema = tableResult.schema ?: return emptyMap()
    return schema.fields.associate {
      val accessor = metadata.columns[it.name]!!
      it.name to BigQueryFieldMapper(
        accessor,
        mapperFactory.factory(schema.fields.get(it.name), mapperFactory.toMetadataFactory(accessor))
                as BigQueryMapper<Any>
      )
    }
  }

  private fun recorded(metadataFactory: Map<String, BigQueryMetadataFactory>, executor: () -> TableResult?) = exec(
    executor
  ) { tableResult ->
    val mappers = mapperFactory.fromSchema(tableResult.schema, metadataFactory)
    tableResult.iterateAll().map {
      mappers.entries.associate { (name, mapper) ->
        name to mapper.map(it.get(name))
      }
    }
  }

  private fun <T : Any> getMetadata(entityRef: KClass<T>) = entityAnnotationReader.metadata(entityRef)

  protected fun <T> exec(executor: () -> TableResult?, transform: (TableResult) -> T?) = executor()?.let {
    transform(it)
  }
}