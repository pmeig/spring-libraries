package pmeig.spring.libraries.jpa.core.column.technical.enabled

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.StateColumns
import java.time.LocalDateTime

@MappedSuperclass
abstract class EnabledStateColumns(
  @Column(nullable = false)
  var enabled: Boolean = false,
  createdBy: String? = null,
  created: LocalDateTime? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): StateColumns(createdBy, created, updatedBy, updated) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as EnabledStateColumns

    return enabled == other.enabled
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + enabled.hashCode()
    return result
  }
}