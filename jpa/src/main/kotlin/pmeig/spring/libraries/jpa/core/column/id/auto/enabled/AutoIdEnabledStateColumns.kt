package pmeig.spring.libraries.jpa.core.column.id.auto.enabled

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.enabled.EnabledStateColumns
import java.time.LocalDateTime

@Suppress("unused")
@MappedSuperclass
abstract class AutoIdEnabledStateColumns(
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null,
  enabled: Boolean = false,
  createdBy: String? = null,
  created: LocalDateTime? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): EnabledStateColumns(enabled, createdBy, created, updatedBy, updated)