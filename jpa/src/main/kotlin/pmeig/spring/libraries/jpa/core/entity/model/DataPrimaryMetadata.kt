package pmeig.spring.libraries.jpa.core.entity.model

import pmeig.spring.libraries.jpa.core.FieldAccessor
import java.lang.reflect.Type

private class SameFieldAccessor: FieldAccessor<Any> {
  override val declared: Class<*>
    get() = Any::class.java
  override val java: Class<Any>
    get() = Any::class.java
  override val type: Type
    get() = java

  override fun set(entity: Any?, value: Any?) {
  }

  override fun get(entity: Any?) = entity
  override val struct: Map<String, FieldAccessor<*>>
    get() = emptyMap()
}

private val sameFieldAccessor = SameFieldAccessor()

data class DataPrimaryMetadata(
  val field: FieldAccessor<Any>? = null,
  val fromEntityColumns: Map<String, FieldAccessor<Any>> = emptyMap(),
  val fromID: Map<String, FieldAccessor<Any>> = emptyMap(),
  val embedded: Boolean = false
) {
  fun get(entity: Any?, column: String) = fromEntityColumns[column]?.get(entity)
  fun getID(id: Any?, column: String) = fromID.ifEmpty { mapOf(column to sameFieldAccessor) }[column]?.get(id)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataPrimaryMetadata

    if (embedded != other.embedded) return false
    if (field != other.field) return false
    if (fromEntityColumns != other.fromEntityColumns) return false
    if (fromID != other.fromID) return false

    return true
  }

  override fun hashCode(): Int {
    var result = embedded.hashCode()
    result = 31 * result + (field?.hashCode() ?: 0)
    result = 31 * result + fromEntityColumns.hashCode()
    result = 31 * result + fromID.hashCode()
    return result
  }


}