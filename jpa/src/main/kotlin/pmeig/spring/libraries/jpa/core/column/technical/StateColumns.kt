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
abstract class StateColumns(
  @CreatedBy
  @Column(name = "created_by", nullable = false)
  var createdBy: String? = null,
  @CreatedDate
  @Column(nullable = false)
  var created: LocalDateTime? = null,
  updatedBy: String? = null,
  updated: LocalDateTime? = null
): UpdatedColumns(updatedBy, updated)