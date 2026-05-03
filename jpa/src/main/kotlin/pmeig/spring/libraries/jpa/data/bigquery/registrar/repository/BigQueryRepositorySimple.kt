package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.TableId
import org.springframework.cache.CacheManager
import org.springframework.core.annotation.AnnotatedElementUtils
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.annotation.Dataset
import pmeig.spring.libraries.jpa.data.bigquery.cache.QueryCache
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory

@Suppress("unused")
class BigQueryRepositorySimple(
  private val client: BigQueryClient,
  private val entityReader: EntityAnnotationReader,
  private val mapperFactory: BigQueryMapperFactory,
  private val cacheManager: CacheManager
) {

  private val cache = mutableMapOf<Class<*>, QueryCache>()

  fun save(entity: Any) = save(listOf(entity)).firstOrNull()
  fun save(entity: Any, vararg entities: Any) = save(listOf(entity) + entities.toList())

  fun save(entities: Iterable<Any>): Collection<Any> {
    val all = entities.toMutableList()
    if (all.isEmpty()) {
      return all
    }
    val entityRef = all.first()
    val queries = getQueries(entityRef)
    val (tmpTable, tableName) = createTmpTable(queries, all)
    val saved = client.tryEntities(entityRef.javaClass, queries.upsertQuery.replace(QueryCache.TABLE_STAGING, tableName))
    client.drop(tmpTable)
    return saved
  }

  fun find(entity: Any) = find(listOf(entity)).firstOrNull()
  fun find(entity: Any, vararg entities: Any) = find(listOf(entity) + entities.toList())
  fun find(entities: Iterable<Any>): Collection<Any> {
    val all = entities.toMutableList()
    if (all.isEmpty()) {
      return all
    }
    val entityRef = all.first()
    val queries = getQueries(entityRef)
    return client.tryEntities(entityRef.javaClass, queries.selectById) {
      it.addNamedParameter("ids", com.google.cloud.bigquery.QueryParameterValue.array(
        entities.map {
          entity -> queries.metadata.primary.fromID.values.joinToString("_") {
            accessor ->
            accessor.get(entity)?.toString() ?: ""
        }
        }.toTypedArray(), StandardSQLTypeName.STRING
      ))
    }
  }

  private fun createTmpTable(
    queries: QueryCache,
    entities: List<Any>
  ): Pair<TableId, String> {
    var table = queries.tableName
    val dataset = queries.dataset.value
    val project = queries.dataset.project
    var index = 1
    while (client.exists(table, dataset, project)) {
      table = queries.tableName + index++
    }
    client.createTable(queries.schema, table, dataset, project)
    client.tryQuery(queries.insert(entities), applyParameters(queries, entities))
    return Pair(client.createTableId(table, dataset, project), queries.table.substringBeforeLast(".") + ".$table")
  }

  private fun applyParameters(
    queries: QueryCache,
    entities: List<Any>,
    filter: Collection<String> = listOf()
  ): (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder {
    var adder = { builder: QueryJobConfiguration.Builder ->
      builder
    }
    val configurers = if (filter.isEmpty()) {
      queries.configurer
    } else queries.configurer.filterKeys { filter.contains(it) }
    entities.forEachIndexed { index, entity ->
      val previous = adder
      configurers.forEach { (_, value) ->
        adder = {
          previous(it).apply {
            value(entity, index)(it)
          }
        }
      }
    }
    return adder
  }

  private fun getQueries(entity: Any): QueryCache = cache.getOrPut(entity.javaClass) {
    val metadata = entityReader.metadata(entity)
    val dataset = AnnotatedElementUtils.findMergedAnnotation(entity.javaClass, Dataset::class.java) ?: Dataset("")
    val project = dataset.project
    val datasetName = dataset.value
    val schema =
      client.getTable(metadata.table, datasetName, project)?.schema ?: error("Table ${metadata.table} not found")
    val mappers = createMappers(schema, metadata.columns)
    QueryCache(metadata, dataset, mappers, schema, prepareConfigurer(metadata, mappers))
  }

  @Suppress("UNCHECKED_CAST")
  private fun createMappers(
    schema: Schema,
    accessors: Map<String, FieldAccessor<*>>
  ): Map<String, BigQueryMapper<Any>> {
    return mapperFactory.fromSchema(schema, accessors.entries.associate {
      it.key to mapperFactory.toMetadataFactory(it.value)
    }) as Map<String, BigQueryMapper<Any>>
  }

  private fun prepareConfigurer(
    metadata: DataMetadata,
    mappers: Map<String, BigQueryMapper<Any>>
  ): Map<String, (entity: Any, index: Int?) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder> {
    return mappers.entries.associate {
      val name = it.key
      val accessor = metadata.columns[name]!!
      name to { entity: Any, index: Int? ->
        { builder: QueryJobConfiguration.Builder ->
          builder.addNamedParameter(name + (index?.toString() ?: ""), it.value.parameter(accessor.get(entity)))
        }
      }
    }
  }
}