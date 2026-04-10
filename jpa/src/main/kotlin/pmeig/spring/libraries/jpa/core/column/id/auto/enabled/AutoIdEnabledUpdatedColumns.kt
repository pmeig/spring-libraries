package pmeig.spring.libraries.jpa.core.column.id.auto.enabled

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.enabled.EnabledUpdatedColumns
import java.time.LocalDateTime

@Suppress("unused")
@MappedSuperclass
abstract class AutoIdEnabledUpdatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null,
  enabled: Boolean = false,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): EnabledUpdatedColumns(enabled, updatedBy, updated)