package pmeig.spring.libraries.jpa.core.column.id.auto.enabled

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.enabled.EnabledUpdatedColumns
import java.time.LocalDateTime

@Suppress("unused")
@MappedSuperclass
abstract class AutoIdEnabledUpdatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null,
  enabled: Boolean = false,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): EnabledUpdatedColumns(enabled, updatedBy, updated) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as AutoIdEnabledUpdatedColumns

    return id == other.id
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (id?.hashCode() ?: 0)
    return result
  }
}