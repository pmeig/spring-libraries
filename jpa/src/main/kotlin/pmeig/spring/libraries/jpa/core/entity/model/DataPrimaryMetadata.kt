package pmeig.spring.libraries.jpa.core.entity.model

import pmeig.spring.libraries.jpa.core.FieldAccessor
import java.lang.reflect.Field

data class DataPrimaryMetadata(
  val field: Field? = null,
  val columns: Map<String, FieldAccessor<*>> = emptyMap(),
  val embedded: Boolean = false
) {
  fun get(entity: Any?, column: String) = columns[column]?.get(entity)
}