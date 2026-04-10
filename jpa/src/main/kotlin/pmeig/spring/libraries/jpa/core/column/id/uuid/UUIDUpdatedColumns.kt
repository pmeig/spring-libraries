package pmeig.spring.libraries.jpa.core.column.id.uuid

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.UpdatedColumns
import java.time.LocalDateTime
import java.util.UUID

@Suppress("unused")
@MappedSuperclass
abstract class UUIDUpdatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  var id: UUID? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null,
): UpdatedColumns(updatedBy, updated)