package pmeig.spring.libraries.jpa.core.column.id.uuid.enabled

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.enabled.EnabledUpdatedColumns
import java.time.LocalDateTime
import java.util.UUID

@Suppress("unused")
@MappedSuperclass
abstract class UUIDEnabledUpdatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  var id: UUID? = null,
  enabled: Boolean = false,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): EnabledUpdatedColumns(enabled, updatedBy, updated)