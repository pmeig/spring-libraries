package pmeig.spring.libraries.jpa.core.column.technical.enabled

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.UpdatedColumns
import java.time.LocalDateTime

@MappedSuperclass
abstract class EnabledUpdatedColumns(
  @Column(nullable = false)
  var enabled: Boolean = false,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): UpdatedColumns(updatedBy, updated)