package pmeig.spring.libraries.jpa.core.column.id.auto

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.StateColumns
import java.time.LocalDateTime

@MappedSuperclass
abstract class AutoIdStateColumns(
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null,
  createdBy: String? = null,
  created: LocalDateTime? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): StateColumns(createdBy, created, updatedBy, updated) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as AutoIdStateColumns

    return id == other.id
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (id?.hashCode() ?: 0)
    return result
  }
}