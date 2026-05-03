package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository

import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.StandardSQLTypeName
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.FluentQuery
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import java.util.Optional
import java.util.function.Function

typealias ID = Any
typealias Entity = Any

class SimpleJpaBigQueryRepository(
  private val client: BigQueryClient,
  private val metadata: DataMetadata
): JpaRepository<Entity, ID> {

  private val idColumns = metadata.primary.fromID.keys.joinToString(" || '_' || ")
  private val idToString: (ID) -> String = {
    metadata.primary.fromID.keys.joinToString("_") { column ->
      metadata.primary.getID(it, column).toString()
    }
  }
  private val entityToIdString: (Entity) -> String = {
    metadata.primary.fromEntityColumns.keys.joinToString("_") { column ->
      metadata.primary.get(it, column).toString()
    }
  }

  override fun flush() {
  }

  override fun <S: Entity> saveAndFlush(entity: S) = save(entity)

  override fun <S: Entity> saveAllAndFlush(entities: Iterable<S>) = saveAll(entities)

  override fun deleteAllInBatch(entities: Iterable<Entity>) =
    deleteAllByIdInBatch(entities.map { metadata.primary.field!!.get(it) as ID })

  override fun deleteAllInBatch() {
    client.tryBatch("DELETE FROM ${metadata.table} WHERE 1 = 1")
  }

  override fun deleteAllByIdInBatch(ids: Iterable<ID>) {
    client.tryBatch("DELETE FROM ${metadata.table} WHERE ${idColumns} IN UNNEST(@ids)") {
      it.addNamedParameter("ids", QueryParameterValue.array(ids.map { id -> idToString(id) }.toTypedArray(), StandardSQLTypeName.STRING))
    }
  }

  override fun getOne(id: ID): Entity = client.tryEntity(metadata.createEntity().javaClass as Class<Entity>, "SELECT * FROM ${metadata.table} WHERE ${idColumns} = @id") {
    it.addNamedParameter("id", QueryParameterValue.string(idToString(id)))
  } ?: throw EntityNotFoundException("Entity not found")

  override fun getById(id: ID): Entity = getOne(id)

  override fun getReferenceById(id: ID): Entity = getOne(id)

  override fun <S: Entity> findAll(example: Example<S>): List<S> {
    return listOf()
  }

  override fun <S: Entity> findAll(
    example: Example<S>,
    sort: Sort
  ): List<S> {
    TODO("Not yet implemented")
  }

  override fun findAll(): List<Entity> {
    TODO("Not yet implemented")
  }

  override fun findAll(sort: Sort): List<Entity> {
    TODO("Not yet implemented")
  }

  override fun findAll(pageable: Pageable): Page<Entity> {
    TODO("Not yet implemented")
  }

  override fun <S: Entity> findAll(
    example: Example<S>,
    pageable: Pageable
  ): Page<S> {
    TODO("Not yet implemented")
  }

  override fun <S: Entity> saveAll(entities: Iterable<S>): List<S> {
    TODO("Not yet implemented")
  }

  override fun findAllById(ids: Iterable<ID>): List<Entity> {
    TODO("Not yet implemented")
  }

  override fun <S: Entity> save(entity: S): S {
    TODO("Not yet implemented")
  }

  override fun findById(id: ID): Optional<in Entity> {
    TODO("Not yet implemented")
  }

  override fun existsById(id: ID): Boolean {
    TODO("Not yet implemented")
  }

  override fun count(): Long {
    TODO("Not yet implemented")
  }

  override fun <S: Entity> count(example: Example<S>): Long {
    TODO("Not yet implemented")
  }

  override fun deleteById(id: ID) {
    TODO("Not yet implemented")
  }

  override fun delete(entity: Entity) {
    TODO("Not yet implemented")
  }

  override fun deleteAllById(ids: Iterable<ID>) {
    TODO("Not yet implemented")
  }

  override fun deleteAll(entities: Iterable<Entity>) {
    TODO("Not yet implemented")
  }

  override fun deleteAll() {
    TODO("Not yet implemented")
  }

  override fun <S: Entity> findOne(example: Example<S>): Optional<S> {
    TODO("Not yet implemented")
  }

  override fun <S: Entity> exists(example: Example<S>): Boolean {
    TODO("Not yet implemented")
  }

  override fun <S: Entity, R> findBy(
    example: Example<S>,
    queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>
  ): R {
    TODO("Not yet implemented")
  }
}