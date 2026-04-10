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
}