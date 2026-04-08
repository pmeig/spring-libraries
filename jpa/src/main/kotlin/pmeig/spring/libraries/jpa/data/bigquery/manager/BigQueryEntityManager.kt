package pmeig.spring.libraries.jpa.data.bigquery.manager

import com.google.cloud.bigquery.TableId
import jakarta.persistence.CacheRetrieveMode
import jakarta.persistence.CacheStoreMode
import jakarta.persistence.ConnectionConsumer
import jakarta.persistence.ConnectionFunction
import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityTransaction
import jakarta.persistence.FindOption
import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.LockOption
import jakarta.persistence.Query
import jakarta.persistence.RefreshOption
import jakarta.persistence.StoredProcedureQuery
import jakarta.persistence.TypedQuery
import jakarta.persistence.TypedQueryReference
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.CriteriaSelect
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.metamodel.Metamodel
import org.springframework.cglib.proxy.Enhancer
import org.springframework.cglib.proxy.MethodInterceptor
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import pmeig.spring.libraries.jpa.data.bigquery.converter.BigQueryTable
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.BigQueryAnnotationConverter
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQuerySQLMetadata
import java.util.concurrent.ConcurrentHashMap

private val tableCache = ConcurrentHashMap<TableId, BigQueryTable>()

@Component
class BigQueryEntityManager(
  private val bigQueryEntityManagerService: BigQueryEntityManagerService,
  private val entityConverter: BigQueryAnnotationConverter,
) : EntityManager {

  @Suppress("UNCHECKED_CAST")
  override fun persist(entity: Any?) {
    entity?.let {
      val metadata = entityConverter.convert(it)
      val (clause, addParameter) = bigQueryEntityManagerService.prepareClause(metadata, it)
      val insert = "INSERT INTO `${metadata.table}`(${
        clause.split("AND").joinToString(", ") { sentence -> sentence.split("=").first() }
      })"
      bigQueryEntityManagerService.execute(insert + " VALUES (${clause})", addParameter)
    }

  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> merge(entity: T?): T? {
    return entity?.let {
      val metadata = entityConverter.convert(it)
      if (metadata.primaryColumn == null) error("Cannot merge an entity without a primary key")
      find(ClassUtils.getUserClass(entity) as Class<T>, metadata.primaryColumn.field.get(it))
        ?.let { before ->
          val columns = metadata.columns.entries.filter { (_, field) ->
            val beforeValue = field.get(before)
            val actualValue = field.get(entity)
            beforeValue != actualValue
          }.map { (column) -> column }
          bigQueryEntityManagerService.prepareClause(metadata, it, columns)
        }?.let { (insert, addParameter) ->
          val (clause, adder) = bigQueryEntityManagerService.prepareClause(metadata, it, metadata.primaryColumn.columns.map { column ->column.key })
          val update = "UPDATE `${metadata.table}` SET $insert " +
                  "WHERE " + clause
          bigQueryEntityManagerService.execute(update) { builder ->
            adder(addParameter(builder))
          }
          it
        } ?: persist(it) as T?
    }
  }

  override fun remove(entity: Any?) {
    entity?.let {
      val metadata = entityConverter.convert(it)
      if (metadata.primaryColumn == null) error("Cannot remove an entity without a primary key")
      val query = "DELETE FROM  `${metadata.table}` WHERE "
      val (clause, addParameter) = bigQueryEntityManagerService.prepareClause(metadata, entity, metadata.primaryColumn.columns
        .map { column -> column.key })
      bigQueryEntityManagerService.execute(query + clause, addParameter)
    }
  }
  override fun <T> find(entityClass: Class<T>, primaryKey: Any?): T? {
    return primaryKey?.let { primary ->
      var metadata = entityConverter.convert(entityClass)
      if (metadata.primaryColumn == null) error("Cannot find an entity without a primary key")
      if (metadata.primaryColumn.embedded) {
        val accessors = entityConverter.extractColumns(ClassUtils.getUserClass(primary)) {}
        metadata = BigQuerySQLMetadata(metadata.table, metadata.primaryColumn, accessors)
      }
      val (clause, addParameter) = bigQueryEntityManagerService.prepareClause(metadata, primary, metadata.primaryColumn!!.columns
        .map { column -> column.key })
      return bigQueryEntityManagerService.execute(entityClass, "SELECT * FROM " +
              "`${metadata.table}` WHERE $clause", addParameter)
    }
  }

  override fun <T> find(
    entityClass: Class<T>,
    primaryKey: Any?,
    properties: Map<String?, Any?>?
  ) = find(entityClass, primaryKey)

  override fun <T> find(
    entityClass: Class<T>,
    primaryKey: Any?,
    lockMode: LockModeType?
  ) = find(entityClass, primaryKey)

  override fun <T> find(
    entityClass: Class<T>,
    primaryKey: Any?,
    lockMode: LockModeType?,
    properties: Map<String?, Any?>?
  ) = find(entityClass, primaryKey)

  override fun <T> find(
    entityClass: Class<T>,
    primaryKey: Any?,
    vararg options: FindOption?
  ) = find(entityClass, primaryKey)

  override fun <T> find(
    entityGraph: EntityGraph<T>,
    primaryKey: Any?,
    vararg options: FindOption?
  ): T? {
    error("Not yet implemented")
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> getReference(entityClass: Class<T>, primaryKey: Any?): T? {
    val enhancer = Enhancer()
    var entity: T? = null
    enhancer.setSuperclass(entityClass)
    enhancer.setCallback(MethodInterceptor { _, method, args, _ ->
      if (null == entity) entity = find(entityClass, primaryKey)
      entity?.let {
        method.invoke(it, *args)
      }
    })
    return enhancer.create() as T?
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> getReference(entity: T?): T? {
    return entity?.let {
      val clazz = ClassUtils.getUserClass(it) as Class<T>
      getReference(clazz, entityConverter.extractId(it, clazz))
    }

  }
  override fun flush() {}

  override fun setFlushMode(flushMode: FlushModeType?) {}

  override fun getFlushMode(): FlushModeType? = FlushModeType.AUTO

  override fun lock(entity: Any?, lockMode: LockModeType?) {}

  override fun lock(
    entity: Any?,
    lockMode: LockModeType?,
    properties: Map<String?, Any?>?
  ) {}

  override fun lock(
    entity: Any?,
    lockMode: LockModeType?,
    vararg options: LockOption?
  ) {}

  override fun refresh(entity: Any?) {}

  override fun refresh(entity: Any?, properties: Map<String?, Any?>?) {}

  override fun refresh(entity: Any?, lockMode: LockModeType?) {}

  override fun refresh(
    entity: Any?,
    lockMode: LockModeType?,
    properties: Map<String?, Any?>?
  ) {}

  override fun refresh(entity: Any?, vararg options: RefreshOption?) {}

  override fun clear() {}

  override fun detach(entity: Any?) {}

  override fun contains(entity: Any?): Boolean {
    return entity?.let {
      bigQueryEntityManagerService.exists(entityConverter.extractId(entity), entityConverter.convert(ClassUtils.getUserClass(it)))
    } ?: false
  }

  override fun getLockMode(entity: Any?): LockModeType? = LockModeType.NONE

  override fun setCacheRetrieveMode(cacheRetrieveMode: CacheRetrieveMode?) {
  }

  override fun setCacheStoreMode(cacheStoreMode: CacheStoreMode?) {
  }

  override fun getCacheRetrieveMode(): CacheRetrieveMode = CacheRetrieveMode.BYPASS

  override fun getCacheStoreMode(): CacheStoreMode = CacheStoreMode.BYPASS

  override fun setProperty(propertyName: String?, value: Any?) {
  }

  override fun getProperties(): Map<String?, Any?> = mapOf()

  override fun createQuery(qlString: String?): Query? {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> createQuery(criteriaQuery: CriteriaQuery<T?>): TypedQuery<T?>? = null

  override fun <T : Any?> createQuery(selectQuery: CriteriaSelect<T?>?): TypedQuery<T?>? {
    TODO("Not yet implemented")
  }

  override fun createQuery(updateQuery: CriteriaUpdate<*>?): Query? {
    TODO("Not yet implemented")
  }

  override fun createQuery(deleteQuery: CriteriaDelete<*>?): Query? {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> createQuery(
    qlString: String?,
    resultClass: Class<T?>?
  ): TypedQuery<T?>? {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> createQuery(reference: TypedQueryReference<T?>?): TypedQuery<T?>? {
    TODO("Not yet implemented")
  }

  override fun createNamedQuery(name: String?): Query? {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> createNamedQuery(
    name: String?,
    resultClass: Class<T?>?
  ): TypedQuery<T?>? {
    TODO("Not yet implemented")
  }

  override fun createNativeQuery(sqlString: String?): Query? {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> createNativeQuery(
    sqlString: String?,
    resultClass: Class<T?>?
  ): Query? {
    TODO("Not yet implemented")
  }

  override fun createNativeQuery(
    sqlString: String?,
    resultSetMapping: String?
  ): Query? {
    TODO("Not yet implemented")
  }

  override fun createNamedStoredProcedureQuery(name: String?): StoredProcedureQuery? {
    TODO("Not yet implemented")
  }

  override fun createStoredProcedureQuery(procedureName: String?): StoredProcedureQuery? {
    TODO("Not yet implemented")
  }

  override fun createStoredProcedureQuery(
    procedureName: String?,
    vararg resultClasses: Class<*>?
  ): StoredProcedureQuery? {
    TODO("Not yet implemented")
  }

  override fun createStoredProcedureQuery(
    procedureName: String?,
    vararg resultSetMappings: String?
  ): StoredProcedureQuery? {
    TODO("Not yet implemented")
  }

  override fun joinTransaction() {
    TODO("Not yet implemented")
  }

  override fun isJoinedToTransaction(): Boolean {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> unwrap(cls: Class<T?>?): T? {
    TODO("Not yet implemented")
  }

  override fun getDelegate(): Any? {
    TODO("Not yet implemented")
  }

  override fun close() {
    TODO("Not yet implemented")
  }

  override fun isOpen(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getTransaction(): EntityTransaction? {
    TODO("Not yet implemented")
  }

  override fun getEntityManagerFactory(): EntityManagerFactory? {
    TODO("Not yet implemented")
  }

  override fun getCriteriaBuilder(): CriteriaBuilder? {
    TODO("Not yet implemented")
  }

  override fun getMetamodel(): Metamodel? {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> createEntityGraph(rootType: Class<T?>?): EntityGraph<T?>? {
    TODO("Not yet implemented")
  }

  override fun createEntityGraph(graphName: String?): EntityGraph<*>? {
    TODO("Not yet implemented")
  }

  override fun getEntityGraph(graphName: String?): EntityGraph<*>? {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> getEntityGraphs(entityClass: Class<T?>?): List<EntityGraph<in T>?>? {
    TODO("Not yet implemented")
  }

  override fun <C : Any?> runWithConnection(action: ConnectionConsumer<C?>?) {
    TODO("Not yet implemented")
  }

  override fun <C : Any?, T : Any?> callWithConnection(function: ConnectionFunction<C?, T?>?): T? {
    TODO("Not yet implemented")
  }
}