package pmeig.spring.libraries.jpa.core.entity.model

import pmeig.spring.libraries.jpa.core.accessor.FieldAccessor
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.BigQueryField
import java.lang.reflect.Field

data class DataPrimaryMetadata(
  val field: Field? = null,
  val columns: Map<String, FieldAccessor<*>> = emptyMap(),
  val embedded: Boolean = false
) {
  val columnNames: Set<String> get() = columns.keys

  fun get(entity: Any?, column: String) = columns[column]?.get(entity)
}