package pmeig.spring.libraries.jpa.core.column.technical

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class UpdatedColumns(
  @LastModifiedBy
  @Column(name = "updated_by")
  var updatedBy: String? = null,
  @LastModifiedDate
  var updated: LocalDateTime? = null
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UpdatedColumns

    if (updatedBy != other.updatedBy) return false
    if (updated != other.updated) return false

    return true
  }

  override fun hashCode(): Int {
    var result = updatedBy?.hashCode() ?: 0
    result = 31 * result + (updated?.hashCode() ?: 0)
    return result
  }
}