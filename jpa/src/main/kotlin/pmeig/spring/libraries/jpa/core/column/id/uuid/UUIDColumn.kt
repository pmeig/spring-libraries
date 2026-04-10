package pmeig.spring.libraries.jpa.core.column.id.uuid

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.UUID

@Suppress("unused")
@MappedSuperclass
abstract class UUIDColumn(
  @Column(name = "reference")
  @Id @get:GeneratedValue(strategy = GenerationType.UUID) var uuid: UUID? = null,
)