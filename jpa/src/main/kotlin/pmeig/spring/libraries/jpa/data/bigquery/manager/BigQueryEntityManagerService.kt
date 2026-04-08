package pmeig.spring.libraries.jpa.data.bigquery.manager

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.TableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableResult
import com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryProperties
import org.springframework.stereotype.Service
import pmeig.spring.libraries.jpa.data.bigquery.BigQueryExecutor
import pmeig.spring.libraries.jpa.data.bigquery.converter.BigQueryTable
import pmeig.spring.libraries.jpa.data.bigquery.converter.BigQueryTableResultConverter
import pmeig.spring.libraries.jpa.data.bigquery.converter.BigQueryTypeConverter
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQuerySQLMetadata
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import java.util.LinkedList
import kotlin.jvm.java

@Service
class BigQueryEntityManagerService(
  private val bigQueryMapperFactory: BigQueryMapperFactory,
  private val bigQueryTypeConverter: BigQueryTypeConverter,
  private val properties: GcpBigQueryProperties,
  private val executor: BigQueryExecutor,
  private val tableResultConverter: BigQueryTableResultConverter,
  private val objectMapper: ObjectMapper
) {
  private val tableCache = mutableMapOf<TableId, BigQueryTable>()

  fun execute(
    query: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder
  ): TableResult? {
    return executor.tryAndExecute(query, configurator)
  }

  fun <T> execute(target: Class<T>, query: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder): T? {
    val result = executeList(target, query, configurator)
    return result.firstOrNull()
  }

  fun <T> executeList(entityClass: Class<T>, query: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder): List<T>
    = objectMapper.convertValue(executeList(query, configurator),
    objectMapper.typeFactory.constructCollectionType(LinkedList::class.java, entityClass))

  fun executeList(query: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder): List<Map<String, Any?>> {
    val result = execute(query, configurator)
    return try {
      result?.let { tableResultConverter.convert(it) } ?: emptyList()
    } catch (e: Exception) {
      error("could not execute query: $query because of ${e.message}")
    }
  }

  fun getMappers(schema: Schema?, metadata: BigQuerySQLMetadata): Map<String, BigQueryMapper<*>> {
    return schema?.fields?.associate { field ->
      field.name to bigQueryMapperFactory.factory(
        field, bigQueryTypeConverter.convert(metadata.columns[field.name]!!.declaringClass)
      )
    } ?: mapOf()
  }

  fun getTable(metadata: BigQuerySQLMetadata): BigQueryTable =
    tableCache.computeIfAbsent(TableId.of(properties.datasetName, metadata.table)) {
      val definition = executor.getTable(it).getDefinition<TableDefinition>()
      BigQueryTable(definition, getMappers(definition.schema, metadata))
    }

  fun prepareClause(
    metadata: BigQuerySQLMetadata,
    entity: Any,
    table: BigQueryTable = getTable(metadata)
  ): Pair<String, (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder> {
    var addParameter: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
    val clause = metadata.columns.entries.filter {
      it.value.get(entity)?.let { value ->
        val previous = addParameter
        addParameter = { builder ->
          previous(builder).addNamedParameter(it.key, table.mapper[it.key]?.parameter(value))
        }
      } != null
    }.joinToString(" AND ") {
      "${it.key} = @${it.key}"
    }
    return Pair(clause, addParameter)
  }

  fun prepareClause(
    metadata: BigQuerySQLMetadata,
    entity: Any,
    columns: Collection<String>
  ): Pair<String, (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder> {
    var addParameter: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
    val table = getTable(metadata)
    val clause = columns.joinToString(" AND ") { column ->
      val previous = addParameter
      addParameter = { builder ->
        previous(builder).addNamedParameter(column, table.mapper[column]!!.parameter(metadata.columns[column]!!.get(entity)))
      }
      "$column = @$column"
    }
    return Pair(clause, addParameter)
  }

  fun exists(primary: Any?, metadata: BigQuerySQLMetadata): Boolean {
    if (metadata.primaryColumn == null) error("Cannot check existence of an entity without a primary key")
    var clause = ""
    var addParameter: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
    val table = getTable(metadata)
    val columns = metadata.primaryColumn.columns.joinToString(", ") {
      clause += " AND ${it.key} = @${it.key}"
      val previous = addParameter
      addParameter = {
        builder -> previous(builder).addNamedParameter(it.key, table
          .mapper[it.key]!!.parameter(it.value[primary]))
      }
      it.key
    }
    val query = "SELECT count($columns) > 0 FROM `${metadata.table}` " +
            "WHERE $clause"
    return execute(Boolean::class.java, query, addParameter) ?: false
  }
}