package pmeig.spring.libraries.jpa.shared.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import pmeig.spring.libraries.jpa.core.annotation.Struct
import pmeig.spring.libraries.jpa.core.column.id.auto.AutoIdStateColumns
import java.time.LocalDateTime

@Suppress("JpaAttributeTypeInspection", "unused", "JpaDataSourceORMInspection")
@Entity(name = "entity_test")
class EntityTest(
  id : Long? = null,
  createdBy: String? = null,
  created: LocalDateTime? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null,
  @Struct
  var struct: StructColumn? = null,
  @Column(name = "secret")
  var secretName: String? = null,
  var column: String? = null,
  var array: List<String>? = null
): AutoIdStateColumns(id, createdBy, created, updatedBy, updated) {
}