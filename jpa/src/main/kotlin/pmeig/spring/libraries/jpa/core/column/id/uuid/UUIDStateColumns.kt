package pmeig.spring.libraries.jpa.core.column.id.uuid

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.StateColumns
import java.time.LocalDateTime
import java.util.UUID

@Suppress("unused")
@MappedSuperclass
abstract class UUIDStateColumns(
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  var id: UUID? = null,
  createdBy: String? = null,
  created: LocalDateTime? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): StateColumns(createdBy, created, updatedBy, updated)