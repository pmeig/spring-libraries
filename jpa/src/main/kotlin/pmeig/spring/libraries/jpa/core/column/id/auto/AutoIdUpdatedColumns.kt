package pmeig.spring.libraries.jpa.core.column.id.auto

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.UpdatedColumns
import java.time.LocalDateTime

@Suppress("unused")
@MappedSuperclass
abstract class AutoIdUpdatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null,
): UpdatedColumns(updatedBy, updated)