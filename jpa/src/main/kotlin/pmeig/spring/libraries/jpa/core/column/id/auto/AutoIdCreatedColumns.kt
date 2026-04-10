package pmeig.spring.libraries.jpa.core.column.id.auto

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import pmeig.spring.libraries.jpa.core.column.technical.CreatedColumns
import java.time.LocalDateTime

@Suppress("unused")
@MappedSuperclass
abstract class AutoIdCreatedColumns(
  @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null,
  createdBy: String? = null,
  created: LocalDateTime? = null,
): CreatedColumns(createdBy, created)