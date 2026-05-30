package pmeig.spring.libraries.jpa.core.column.id.uuid

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.CreatedColumns
import java.time.LocalDateTime

@Suppress("unused")
@MappedSuperclass
abstract class UUIDCreatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  var id: Long? = null,
  createdBy: String? = null,
  created: LocalDateTime? = null,
): CreatedColumns(createdBy, created) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as UUIDCreatedColumns

    return id == other.id
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (id?.hashCode() ?: 0)
    return result
  }
}