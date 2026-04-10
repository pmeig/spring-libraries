package pmeig.spring.libraries.jpa.core.column.id.auto

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

@Suppress("unused")
@MappedSuperclass
abstract class AutoIdColumn(
  @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null,
)