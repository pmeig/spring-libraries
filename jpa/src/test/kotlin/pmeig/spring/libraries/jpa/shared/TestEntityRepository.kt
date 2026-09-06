package pmeig.spring.libraries.jpa.shared

import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pmeig.spring.libraries.jpa.data.bigquery.annotation.BigQueryRepository
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import java.time.LocalDateTime

@Suppress("JpaQlInspection", "unused")
@BigQueryRepository
interface TestEntityRepository: JpaRepository<EntityTest, Long> {

  fun errorNoQuery(): String

  fun batchNoQuery(): String

  @Query("SELECT secret FROM entity_test WHERE id = ?1 AND secret = ?2")
  fun queryWithPositional(id: Long, label: String): String

  @Query("SELECT secret FROM entity_test WHERE secret = :name")
  fun queryWithNamed(@Param("name") name: String): String

  @Query("SELECT entity_test FROM entity_test WHERE id = :id")
  fun queryToJson(@Param("id") id: Long): String

  fun findBySecretName(secret: String): EntityTest
  fun findBySecretNameAndColumn(secret: String, column: String): Optional<EntityTest>
  fun findBySecretNameOrColumn(name: String, column: String): EntityTest
  fun findAllByCreated(created: LocalDateTime): EntityTest
  fun findAllBySecretNameLike(name: String): List<EntityTest>
  fun findAllBySecretNameIsNull(): List<EntityTest>
  fun findAllBySecretNameIsNotNull(): List<EntityTest>
  fun findAllBySecretNameIn(names: Collection<String>): List<EntityTest>
}