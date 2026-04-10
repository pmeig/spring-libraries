package pmeig.spring.libraries.jpa.core.column.technical.enabled

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class EnabledColumn(
  @Column(nullable = false)
  var enabled: Boolean = false,
)