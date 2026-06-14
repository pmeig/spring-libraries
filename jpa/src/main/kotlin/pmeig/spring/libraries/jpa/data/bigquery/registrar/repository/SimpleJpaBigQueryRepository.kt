package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.StandardSQLTypeName
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.NoResultException
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.DeleteSpecification
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.UpdateSpecification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.FluentQuery
import pmeig.spring.libraries.jpa.core.addOrder
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.converter.specification.SpecificationReader
import pmeig.spring.libraries.jpa.core.converter.specification.model.SpecificationContext
import pmeig.spring.libraries.jpa.core.converter.specification.model.SpecificationParameter
import pmeig.spring.libraries.jpa.core.createSqlPage
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper
import pmeig.spring.libraries.jpa.data.bigquery.mappers
import pmeig.spring.libraries.jpa.core.executor.JpaMethodInvoker
import pmeig.spring.libraries.jpa.core.executor.JpaMethodInvokerResult
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.time.Clock
import java.time.Instant
import java.util.Optional
import java.util.function.Function

@Suppress("UNCHECKED_CAST")
class SimpleJpaBigQueryRepository<Entity: Any, ID: Any>(
  private val client: BigQueryClient,
  private val cacheManager: DataCacheManager,
  entityAnnotationReader: EntityAnnotationReader,
  clazz: Class<*>
): JpaRepository<Entity, ID>, JpaSpecificationExecutor<Entity>, JpaMethodInvoker {

  private val metadata: DataMetadata = entityAnnotationReader.metadata(clazz)
  @Suppress("UNCHECKED_CAST")
  private val specificationReader = SpecificationReader(metadata.reference as Class<Entity>)

  private val idColumns = metadata.primary.fromID.ifEmpty { metadata.primary.fromEntityColumns }.keys.joinToString(" || '_' || ")
  private val idToString: (ID) -> String = {
    metadata.primary.fromID.ifEmpty { metadata.primary.fromEntityColumns }.keys.joinToString("_") { column ->
      metadata.primary.getID(it, column).toString()
    }
  }
  private val entityToIdString: (Entity) -> String = {
    metadata.primary.fromEntityColumns.keys.joinToString("_") { column ->
      metadata.primary.get(it, column).toString()
    }
  }

  private var mappers: Map<String, BigQueryFieldMapper<*>> = emptyMap()
    get() {
      if (field.isEmpty()) {
        field = mappers(cacheManager, metadata) {
          client.tryEntity(metadata.reference.kotlin, "SELECT * FROM ${metadata.table}") { it.setMaxResults(1) }
          mappers(cacheManager, metadata) { emptyMap() }
        }
      }
      return field
    }

  override fun invokeMethod(method: Method, returnType: Type, args: Array<Any?>) = try {
    SimpleJpaBigQueryRepository::class.java.getDeclaredMethod(method.name, *method.parameterTypes)
    JpaMethodInvokerResult(method.invoke(this, *args))
  } catch (_: NoSuchMethodException) {
    JpaMethodInvokerResult()
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
    client.tryBatch("DELETE FROM ${metadata.table} WHERE $idColumns IN UNNEST(@ids)") {
      it.addNamedParameter(
        "ids",
        QueryParameterValue.array(ids.map { id -> idToString(id) }.toTypedArray(), StandardSQLTypeName.STRING)
      )
    }
  }

  @Deprecated("Use getReferenceById instead")
  override fun getOne(id: ID): Entity = getReferenceById(id)

  @Deprecated("Use getReferenceById instead")
  override fun getById(id: ID): Entity = getReferenceById(id)

  override fun getReferenceById(id: ID): Entity = findById(id).orElseThrow { EntityNotFoundException() } as Entity

  @Suppress("UNCHECKED_CAST")
  override fun <S: Entity> findAll(example: Example<S>): List<S> = findAll(example, Sort.unsorted())

  @Suppress("UNCHECKED_CAST")
  override fun <S: Entity> findAll(
    example: Example<S>,
    sort: Sort
  ): List<S> = fromExample(example).let {
    client.tryEntities(example.probeType, addOrder(it.sql, sort)) { builder ->
      applyExampleParameters(it.parameters, builder)
    }
  }

  override fun findAll() = findAll(Sort.unsorted())

  override fun findAll(sort: Sort): List<Entity> =
    client.tryEntities(metadata.reference, addOrder("SELECT * FROM ${metadata.table}", sort)) as List<Entity>

  override fun findAll(pageable: Pageable): Page<Entity> = executePageableQuery(pageable, count())

  @Suppress("UNCHECKED_CAST")
  override fun <S: Entity> findAll(
    example: Example<S>,
    pageable: Pageable
  ): Page<S> {
    val spec = extractFrom(example)
    return executePageableQuery(pageable, count(example), spec, example.probeType)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <S: Entity> saveAll(entities: Iterable<S>): List<S> {
    val numberEntity = entities.count()
    if (numberEntity < 3) {
      return entities.map { save(it) }.toList()
    }
    val saved = mutableListOf(save(entities.first()))
    val insertableEntities = entities.drop(1)
    val tableName = createTemporaryTable(insertableEntities.first())
    insertTmpTable(insertableEntities.subList(1, insertableEntities.size), tableName)
    val query = createQueryUpsert("SELECT * FROM $tableName")
    saved.addAll(
      client.tryEntities(metadata.reference, query)
        .ifEmpty { throw NoResultException("Could not save entities") } as Collection<S>)
    return saved
  }

  override fun findAllById(ids: Iterable<ID>): List<Entity> {
    return client.tryEntities(
      metadata.reference,
      "SELECT * FROM ${metadata.table} WHERE $idColumns IN UNNEST(@ids)"
    ) {
      it.addNamedParameter(
        "ids",
        QueryParameterValue.array(ids.map { id -> idToString(id) }.toTypedArray(), StandardSQLTypeName.STRING)
      )
    } as List<Entity>
  }

  override fun <S: Entity> save(entity: S): S {
    val query = createQueryUpsert("SELECT ${metadata.columns.all.keys.joinToString(",") { "@$it as $it" }}")
    return client.tryEntity(entity.javaClass.kotlin, query) {
      mappers.entries.forEach { (name, mapper) ->
        it.addNamedParameter(name, mapper.parameter(entity))
      }
      it
    } ?: throw EntityNotFoundException()
  }

  override fun findById(id: ID): Optional<Entity> = Optional.ofNullable(
    client.tryEntity(
      metadata.reference.kotlin,
      "SELECT * FROM ${metadata.table} WHERE $idColumns = @id"
    ) {
      it.addNamedParameter("id", QueryParameterValue.string(idToString(id)))
    }) as Optional<Entity>

  override fun existsById(id: ID): Boolean = findById(id).isPresent

  override fun count(): Long = client.trySingle(
    Long::class,
    "SELECT COUNT(${metadata.primary.fromEntityColumns.keys.joinToString(" || '_' || ")}) FROM ${metadata.table}"
  ) ?: 0L

  override fun <S: Entity> count(example: Example<S>): Long = extractFrom(example).let { context ->
    client.trySingle(Long::class, "SELECT COUNT($idColumns) ${context.sql}") {
      applyExampleParameters(context.parameters, it)
    } ?: 0L
  }

  override fun deleteById(id: ID) {
    client.tryQuery("DELETE FROM ${metadata.table} WHERE $idColumns = @id") {
      it.addNamedParameter("id", QueryParameterValue.string(idToString(id)))
    }
  }

  override fun delete(entity: Entity) {
    client.tryQuery("DELETE FROM ${metadata.table} WHERE $idColumns = @id") {
      it.addNamedParameter("id", QueryParameterValue.string(entityToIdString(entity)))
    }
  }

  override fun deleteAllById(ids: Iterable<ID>) {
    client.tryQuery("DELETE FROM ${metadata.table} WHERE $idColumns IN UNNEST(@id)") {
      var builder = it
      for (id in ids) {
        builder = builder.addNamedParameter("id", QueryParameterValue.string(idToString(id)))
      }
      builder
    }
  }

  override fun deleteAll(entities: Iterable<Entity>) {
    client.tryQuery("DELETE FROM ${metadata.table} WHERE $idColumns IN UNNEST(@id)") {
      var builder = it
      for (entity in entities) {
        builder = builder.addNamedParameter(
          "id",
          QueryParameterValue.string(entityToIdString(entity))
        )
      }
      builder
    }
  }

  override fun deleteAll() {
    client.tryQuery("DELETE FROM ${metadata.table} WHERE 1 = 1")
  }

  @Suppress("UNCHECKED_CAST")
  override fun <S: Entity> findOne(example: Example<S>): Optional<S> = Optional
    .ofNullable(fromExample(example).let {
      client.tryEntity(example.probeType.kotlin, it.sql) { builder ->
        applyExampleParameters(it.parameters, builder.setMaxResults(1))
      }
    })

  override fun <S: Entity> exists(example: Example<S>) = findOne(example).isPresent

  override fun <S: Entity, R> findBy(
    example: Example<S>,
    queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>
  ): R {
    throw UnsupportedOperationException()
  }

  override fun findAll(spec: Specification<Entity>): List<Entity> {
    return findAll(spec, Sort.unsorted())
  }

  override fun findAll(
    spec: Specification<Entity>,
    pageable: Pageable
  ) = findAll(spec, spec, pageable)

  override fun findAll(
    spec: Specification<Entity>,
    countSpec: Specification<Entity>,
    pageable: Pageable
  ): Page<Entity> {
    val count = count(countSpec)
    val context = extractFrom(spec)
    return executePageableQuery(pageable, count, context)
  }

  @Suppress("UNCHECKED_CAST")
  override fun findAll(
    spec: Specification<Entity>,
    sort: Sort
  ): List<Entity> {
    return fromSpecification(spec).let {
      client.tryEntities(metadata.reference.kotlin, addOrder(it.sql, sort)) { builder ->
        applyExampleParameters(it.parameters, builder)
      } as List<Entity>
    }
  }

  override fun count(spec: Specification<Entity>): Long {
    return extractFrom(spec).let {
      client.trySingle(Long::class, "SELECT COUNT(*) $it") ?: 0L
    }
  }

  override fun delete(spec: DeleteSpecification<Entity>): Long {
    return specificationReader.specToSql(spec).let {
      client.tryQuery("DELETE FROM ${metadata.table} " +
              "WHERE ${it.sql.substringAfter(" WHERE ")} THEN RETURN *")?.totalRows ?: 0L
    }
  }

  override fun findOne(spec: Specification<Entity>): Optional<Entity> {
    return fromSpecification(spec).let {
      Optional.ofNullable(client.tryEntity(metadata.reference.kotlin, it.sql))
    }
  }

  override fun exists(spec: Specification<Entity>): Boolean {
    return count(spec) > 0
  }

  override fun <S: Entity, R> findBy(
    spec: Specification<Entity>,
    queryFunction: Function<in JpaSpecificationExecutor.SpecificationFluentQuery<S>, R>
  ): R {
    throw UnsupportedOperationException()
  }

  override fun update(spec: UpdateSpecification<Entity>): Long {
    return specificationReader.specToSql(spec).let {
      client.tryQuery("UPDATE ${metadata.table} " +
              "SET ${it.sql.substringAfter(" SET ")} THEN RETURN *")?.totalRows ?: 0L
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <S: Entity> executePageableQuery(
    pageable: Pageable,
    total: Long,
    spec: SpecificationContext = SpecificationContext(metadata.table, listOf()),
    entityReference: Class<S> = metadata.reference as Class<S>
  ): PageImpl<S> {
    val sql = createSqlPage(metadata, pageable, spec.sql)
    return client.tryEntities(entityReference, sql) {
      applyExampleParameters(spec.parameters, it.setMaxResults(pageable.pageSize.toLong()))
    }.let { PageImpl(it, pageable, total) }
  }

  private fun createQueryUpsert(using: String): String = """
      MERGE INTO ${metadata.table} AS target
      USING ($using) AS source
      ON target.id = source.id
      WHEN MATCHED THEN
          UPDATE SET ${metadata.columns.updated.keys.joinToString(",") { "target.$it = source.$it" }}
          THEN RETURN *
      WHEN NOT MATCHED THEN
          INSERT (${metadata.columns.all.keys.joinToString(",")})
          VALUES (${metadata.columns.all.keys.joinToString(",") { "source.$it" }})
          THEN RETURN *
      """

  private fun <S: Entity> extractFrom(example: Example<S>) = specificationReader.specToSql(example).let {
    SpecificationContext(createFromProvideSpecification(it.sql), it.parameters)
  }

  private fun <S: Entity> extractFrom(spec: Specification<S>) = specificationReader.specToSql(spec).let {
    SpecificationContext(createFromProvideSpecification(it.sql), it.parameters)
  }

  private fun <S: Entity> fromExample(example: Example<S>): SpecificationContext
  = correctSqlGenerateBySpecification(specificationReader.specToSql(example))

  private fun <S: Entity> fromSpecification(spec: Specification<S>)
  = correctSqlGenerateBySpecification(specificationReader.specToSql(spec))

  private fun correctSqlGenerateBySpecification(context: SpecificationContext)
  = SpecificationContext(transformSpecificationSql(context.sql), context.parameters)

  private fun transformSpecificationSql(sql: String): String {
    val afterFrom = createFromProvideSpecification(sql)
    return "${sql.substringBefore(" from ")}$afterFrom"
  }

  private fun createFromProvideSpecification(
    sql: String
  ): String {
    val afterFrom = sql.substringAfter(" from ")
    return " FROM ${metadata.table} " + afterFrom.substringAfter(" ")
  }

  private fun <S: Entity> insertTmpTable(entities: List<S>, tableName: String) {
    var index = 0
    val insertEntities = """
      INSERT INTO $tableName (${metadata.columns.all.keys.joinToString(",")})
      VALUES ${
      entities.joinToString(",") {
        val value = "( ${metadata.columns.all.keys.joinToString(",") { "@$it$index" }} ) "
        index++
        value
      }
    }
    """
    client.tryQuery(insertEntities) {
      entities.forEachIndexed { index, entity ->
        mappers.entries.forEach { (name, mapper) ->
          it.addNamedParameter(name + index, mapper.parameter(entity))
        }
      }
      it
    } ?: throw NoResultException("Could not insert temporary table")
  }

  private fun <S: Entity> createTemporaryTable(entity: S): String {
    val tableName = "${metadata.table.substringBeforeLast('`')}_${Instant.now(Clock.systemUTC()).toEpochMilli()}`"
    val createTableQuery = """
      CREATE TEMP TABLE $tableName  AS 
      (SELECT ${metadata.columns.all.keys.joinToString(",") { "@$it as $it" }})
    """
    client.tryQuery(createTableQuery) {
      mappers.entries.forEach { (name, mapper) ->
        it.addNamedParameter(name, mapper.parameter(entity))
      }
      it
    } ?: throw NoResultException("Could not create temporary table")
    return tableName
  }

  private fun applyExampleParameters(
    parameters: List<SpecificationParameter>,
    builder: QueryJobConfiguration.Builder
  ): QueryJobConfiguration.Builder {
    var addParameterBuilder = builder
    for (parameter in parameters) {
      addParameterBuilder =
        addParameterBuilder.addPositionalParameter(QueryParameterValue.of(parameter.value, parameter.type))
    }
    return addParameterBuilder
  }
}