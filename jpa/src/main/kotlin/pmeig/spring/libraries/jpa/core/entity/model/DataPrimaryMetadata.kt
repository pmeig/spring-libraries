package pmeig.spring.libraries.jpa.core.entity.model

import pmeig.spring.libraries.jpa.core.FieldAccessor
import java.lang.reflect.Field

data class DataPrimaryMetadata(
  val field: Field? = null,
  val columns: Map<String, FieldAccessor<Any>> = emptyMap(),
  val embedded: Boolean = false
) {
  fun get(entity: Any?, column: String) = columns[column]?.get(entity)
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataPrimaryMetadata

    if (embedded != other.embedded) return false
    if (field != other.field) return false
    if (columns != other.columns) return false

    return true
  }

  override fun hashCode(): Int {
    var result = embedded.hashCode()
    result = 31 * result + (field?.hashCode() ?: 0)
    result = 31 * result + columns.hashCode()
    return result
  }

}