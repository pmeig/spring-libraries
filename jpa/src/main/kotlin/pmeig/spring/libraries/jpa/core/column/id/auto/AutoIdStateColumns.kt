package pmeig.spring.libraries.jpa.core.column.id.auto

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.StateColumns
import java.time.LocalDateTime

@MappedSuperclass
abstract class AutoIdStateColumns(
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null,
  createdBy: String? = null,
  created: LocalDateTime? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): StateColumns(createdBy, created, updatedBy, updated)