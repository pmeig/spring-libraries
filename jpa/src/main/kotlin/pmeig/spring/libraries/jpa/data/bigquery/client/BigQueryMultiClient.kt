package pmeig.spring.libraries.jpa.data.bigquery.client

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.TableResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.BigQueryPage
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQuerySqlMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperProvider
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import pmeig.spring.libraries.jpa.data.bigquery.mappers
import kotlin.reflect.KClass

@Suppress("unused")
abstract class BigQueryMultiClient(
  private val mapperFactory: BigQueryMapperFactory,
  private val entityAnnotationReader: EntityAnnotationReader,
  private val sqlMapper: BigQuerySqlMapper,
  protected val cacheManager: DataCacheManager
) {

  abstract fun query(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult

  abstract fun tryQuery(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult?

  @JvmOverloads
  @Suppress("UNCHECKED_CAST")
  fun <T: Any> multiple(target: KClass<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it })
   = query(sql, configurator).iterateAll().map {
     it.firstOrNull()?.let { value ->
       if (value.isNull) null else BigQueryMapperProvider.from<T>(target.javaObjectType).mapper.map(value) as T
     }
   }

  @JvmOverloads
  @Suppress("UNCHECKED_CAST")
  fun <T: Any> tryMultiple(target: KClass<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }) =
    tryQuery(sql, configurator)?.iterateAll()?.map {
      it.firstOrNull()?.let { value ->
        if (value.isNull) null else BigQueryMapperProvider.from<T>(target.javaObjectType).mapper.map(value) as T
      }
    } ?: emptyList()

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

  @JvmOverloads
  fun <T : Any> tryEntities(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef.kotlin, sql, configurator)

  @JvmOverloads
  fun <T : Any> tryEntities(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    toEntity(entityRef) { tryQuery(sql, configurator) } ?: emptyList()

  @JvmOverloads
  fun <T : Any> entities(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef.kotlin, sql, configurator)

  @JvmOverloads
  fun <T : Any> entities(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    toEntity(entityRef) { query(sql, configurator) } ?: emptyList()

  @JvmOverloads
  fun <T : Any> entities(
    entityRef: KClass<T>,
    pageable: Pageable,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Page<T> {
    val metadata = getMetadata(entityRef)
    val tableResult = query(sqlMapper.pageQuery(sql, pageable, metadata), configurator)
    val result = tableResult.iterateAll().first()
    val items = result["items"].repeatedValue!!.toList()
    val total = result["total"].longValue
    val entities = toEntity(entityRef, metadata) {
      TableResult.newBuilder()
        .setSchema(Schema.of(tableResult.schema!!.fields["items"].subFields))
        .setPageNoSchema(BigQueryPage(items))
        .setTotalRows(items.size.toLong())
        .build()
    }!!
    return PageImpl(entities, pageable, total)
  }


  @Suppress("UNCHECKED_CAST")
  private fun <T : Any> toEntity(entity: KClass<T>, metadata: DataMetadata = getMetadata(entity), executor: () -> TableResult?) = exec(executor) { tableResult ->
    tableResult.iterateAll().map {
      val newEntity = metadata.createEntity() as T
      val mappers = mappers(cacheManager, metadata) {
        createEntityFieldMappers(entity, tableResult, metadata)
      }
      mappers.forEach { (fieldName, mapper) ->
        val fieldValue = it[fieldName]
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
      val accessor = metadata.columns.all[it.name]
      it.name to BigQueryFieldMapper(
        accessor!!,
        mapperFactory.factory(schema.fields[it.name], mapperFactory.toMetadataFactory(accessor))
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
        name to mapper.map(it[name])
      }
    }
  }

  private fun <T : Any> getMetadata(entityRef: KClass<T>) = entityAnnotationReader.metadata(entityRef)

  protected fun <T> exec(executor: () -> TableResult?, transform: (TableResult) -> T?) = executor()?.let {
    transform(it)
  }
}