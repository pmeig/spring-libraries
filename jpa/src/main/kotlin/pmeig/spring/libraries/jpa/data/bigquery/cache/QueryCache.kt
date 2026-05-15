package pmeig.spring.libraries.jpa.data.bigquery.cache

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.annotation.Dataset
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper

@Suppress("unused")
class QueryCache(
  val metadata: DataMetadata,
  dataset: Dataset,
  val mappers: Map<String, BigQueryMapper<Any>>,
  val schema: Schema,
  val configurer: Map<String, (entity: Any, index: Int?) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder>
) {

  companion object {
    const val TABLE_STAGING = ":tableStaging:"
  }

  val tableName = metadata.table
  val table = "`${finishByPointIfNotEmpty(dataset.project)}${finishByPointIfNotEmpty(dataset.value)}${tableName}`"
  val selectById = "SELECT ${metadata.columns.keys.joinToString(",")} FROM `${metadata.table}` WHERE ${
    metadata.primary.fromID.keys.joinToString(" || '_' || ")
  } in (@ids)"

  private val insertQueryStart = "INSERT INTO $table (${metadata.columns.keys.joinToString(",")}) " +
          "VALUES "
  val upsertQuery = "MERGE $table AS table " +
          "USING (SELECT * FROM `${TABLE_STAGING}`) AS staging " +
          "ON ${metadata.primary.fromID.keys
            .joinToString(" AND ") {
              "table.${it} = staging.${it}"
            }}" +
          "WHEN MATCHED THEN UPDATE SET " +
          metadata.columns.keys.joinToString(",") { "table.${it} = staging.${it}" } +
          "WHEN NOT MATCHED THEN INSERT (${metadata.columns.keys.joinToString(",")}) VALUES " +
          "(${metadata.columns.keys.joinToString(",") {
            "staging.${it}"
          }})"

  fun insert(entities: List<Any>): String {
    val values = List(entities.size) { index ->
      "(${metadata.columns.keys.joinToString(",") { "@$it$index" }})"
    }
    return insertQueryStart + values.joinToString(",")
  }

  private fun finishByPointIfNotEmpty(name: String) = if(name.isNotEmpty()) "$name." else ""
}