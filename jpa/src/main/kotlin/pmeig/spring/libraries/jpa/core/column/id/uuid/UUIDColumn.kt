package pmeig.spring.libraries.jpa.core.column.id.uuid

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@Suppress("unused")
@MappedSuperclass
abstract class UUIDColumn(
  @Column(name = "reference")
  @Id @GeneratedValue(strategy = GenerationType.UUID) var uuid: UUID? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UUIDColumn

    return uuid == other.uuid
  }

  override fun hashCode(): Int {
    return uuid?.hashCode() ?: 0
  }
}