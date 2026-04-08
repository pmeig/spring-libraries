package pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model

import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.BigQueryField
import java.lang.reflect.Field

data class BigQueryPrimaryKey(
  val field: Field,
  val columns: Set<Map.Entry<String, BigQueryField>>,
  val embedded: Boolean = false
)
