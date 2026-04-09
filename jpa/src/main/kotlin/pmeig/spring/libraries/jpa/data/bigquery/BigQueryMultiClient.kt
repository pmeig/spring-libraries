package pmeig.spring.libraries.jpa.data.bigquery

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import pmeig.spring.libraries.jpa.core.accessor.FieldAccessor
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.cache.BigQueryEntityCache
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import kotlin.reflect.KClass

@Suppress("unused")
abstract class BigQueryMultiClient(
  private val mapperFactory: BigQueryMapperFactory,
  private val entityAnnotationReader: EntityAnnotationReader
) {
  private val cacheMappers = mutableMapOf<KClass<*>, BigQueryEntityCache<*>>()
  abstract fun query(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult

  abstract fun tryQuery(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): TableResult?

  fun records(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>> {
    return recorded {
      query(sql, configurator)
    } ?: emptyList()
  }

  fun tryRecords(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Collection<Map<String, Any?>> {
    return recorded {
      tryQuery(sql, configurator)
    } ?: emptyList()
  }

  fun <T: Any> tryEntities(entityRef: Class<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }) =
    entities(entityRef.kotlin, sql, configurator)
  fun <T : Any> tryEntities(entityRef: KClass<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }) =
    toEntity(entityRef) { tryQuery(sql, configurator) } ?: emptyList()

  fun <T: Any> entities(entityRef: Class<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }) =
    entities(entityRef.kotlin, sql, configurator)
  fun <T : Any> entities(entityRef: KClass<T>, sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }) =
    toEntity(entityRef) { query(sql, configurator) } ?: emptyList()

  @Suppress("UNCHECKED_CAST")
  private fun <T : Any> toEntity(entity: KClass<T>, executor: () -> TableResult?) = exec(executor) { tableResult ->
    val entityMetadata = cacheMappers.getOrPut(entity) {
      val entityFieldMappers = createEntityFieldMappers(entity, tableResult).ifEmpty { error("No columns found for $entity") }
      val constructor = extractEmptyConstructor(entity.javaObjectType)
      BigQueryEntityCache(constructor, entityFieldMappers)
    }
    tableResult.iterateAll().map {
      val newEntity = entityMetadata.constructor.newInstance() as T
      entityMetadata.fieldMappers.forEach { (fieldName, mapper) ->
        val fieldValue = it.get(fieldName)
        mapper.map(newEntity, fieldValue)
      }
      newEntity
    }
  }

  private fun <T: Any> extractEmptyConstructor(clazz: Class<T>) =
    clazz.declaredConstructors.firstOrNull { it.parameterCount == 0 }?.apply {
      isAccessible = true
    } ?: error("No empty constructor found for $clazz")

  @Suppress("UNCHECKED_CAST")
  private fun <T : Any> createEntityFieldMappers(
    entity: KClass<T>,
    tableResult: TableResult
  ): Map<String, BigQueryFieldMapper<*>> {
    val metadata = entityAnnotationReader.metadata(entity)
    val schema = tableResult.schema ?: return emptyMap()
    return metadata.columns.entries.associate {
      it.key to BigQueryFieldMapper(it.value as FieldAccessor<Any>,
        mapperFactory.factory(schema.fields.get(it.key), mapperFactory.toMetadataFactory(it.value))
                as BigQueryMapper<Any>)
    }
  }

  private fun recorded(executor: () -> TableResult?) = exec(
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