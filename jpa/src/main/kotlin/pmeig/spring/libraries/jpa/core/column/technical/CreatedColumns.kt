package pmeig.spring.libraries.jpa.core.column.technical

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class CreatedColumns(
  @CreatedBy
  @Column(name = "created_by")
  var createdBy: String? = null,
  @CreatedDate
  var created: LocalDateTime? = null
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CreatedColumns

    if (createdBy != other.createdBy) return false
    if (created != other.created) return false

    return true
  }

  override fun hashCode(): Int {
    var result = createdBy?.hashCode() ?: 0
    result = 31 * result + (created?.hashCode() ?: 0)
    return result
  }
}