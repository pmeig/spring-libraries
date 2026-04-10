package pmeig.spring.libraries.jpa.core.column.id.uuid.enabled

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.enabled.EnabledColumn
import java.util.UUID

@Suppress("unused")
@MappedSuperclass
abstract class UUIDEnabledColumn(
  @Id @GeneratedValue(strategy = GenerationType.UUID)
  var id: UUID? = null,
  enabled: Boolean = false
): EnabledColumn(enabled)