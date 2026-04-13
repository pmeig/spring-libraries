package pmeig.spring.libraries.jpa.core.column.technical.enabled

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class EnabledColumn(
  @Column(nullable = false)
  var enabled: Boolean = false,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EnabledColumn

    return enabled == other.enabled
  }

  override fun hashCode(): Int {
    return enabled.hashCode()
  }
}