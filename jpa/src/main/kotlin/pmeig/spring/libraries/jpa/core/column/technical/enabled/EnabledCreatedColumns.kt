package pmeig.spring.libraries.jpa.core.column.technical.enabled

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.CreatedColumns
import java.time.LocalDateTime

@MappedSuperclass
abstract class EnabledCreatedColumns(
  @Column(nullable = false)
  var enabled: Boolean = false,
  createdBy: String? = null,
  created: LocalDateTime? = null
): CreatedColumns(createdBy, created)