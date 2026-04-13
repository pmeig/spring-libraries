package pmeig.spring.libraries.jpa.core.column.id.uuid.enabled

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.enabled.EnabledUpdatedColumns
import java.time.LocalDateTime
import java.util.UUID

@Suppress("unused")
@MappedSuperclass
abstract class UUIDEnabledUpdatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  var id: UUID? = null,
  enabled: Boolean = false,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): EnabledUpdatedColumns(enabled, updatedBy, updated) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as UUIDEnabledUpdatedColumns

    return id == other.id
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (id?.hashCode() ?: 0)
    return result
  }
}