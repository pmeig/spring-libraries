package pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model

import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.BigQueryField


data class BigQuerySQLMetadata(
  val table: String,
  val primaryColumn: BigQueryPrimaryKey?,
  val columns: Map<String, BigQueryField<*>> = mapOf(),
)