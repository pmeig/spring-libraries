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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as EntityTest

    if (struct != other.struct) return false
    if (secretName != other.secretName) return false
    if (column != other.column) return false
    if (array != other.array) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (struct?.hashCode() ?: 0)
    result = 31 * result + (secretName?.hashCode() ?: 0)
    result = 31 * result + (column?.hashCode() ?: 0)
    result = 31 * result + (array?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "EntityTest(struct=$struct, secretName=$secretName, column=$column, array=$array," +
            " createdBy=$created, updatedBy=$updated, created=$created, updated=$updated, id=$id)"
  }


}