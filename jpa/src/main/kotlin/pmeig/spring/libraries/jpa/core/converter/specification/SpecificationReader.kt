package pmeig.spring.libraries.jpa.core.converter.specification

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.hibernate.Session
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.query.MutationQuery
import org.hibernate.query.Query
import org.hibernate.query.criteria.JpaCriteriaQuery
import org.hibernate.query.criteria.JpaCriteriaUpdate
import org.hibernate.query.hql.spi.SqmQueryImplementor
import org.hibernate.query.sqm.internal.DomainParameterXref
import org.hibernate.query.sqm.sql.SqmTranslation
import org.hibernate.query.sqm.tree.SqmDmlStatement
import org.hibernate.query.sqm.tree.SqmStatement
import org.hibernate.query.sqm.tree.select.SqmSelectStatement
import org.hibernate.sql.ast.tree.Statement
import org.hibernate.sql.ast.tree.select.SelectStatement
import org.hibernate.sql.exec.spi.JdbcOperation
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Example
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder
import org.springframework.data.jpa.domain.DeleteSpecification
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.UpdateSpecification
import org.springframework.data.jpa.repository.query.EscapeCharacter
import pmeig.spring.libraries.jpa.core.converter.specification.model.SpecificationContext
import pmeig.spring.libraries.jpa.core.converter.specification.model.SpecificationParameter

private val logger = LoggerFactory.getLogger("org.hibernate") as Logger
private val level = logger.level
private var metadataBuilder = StandardServiceRegistryBuilder()
  .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
  .applySetting("hibernate.hbm2ddl.auto", "none")
  .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
  .applySetting("hibernate.show_sql", "false")
  .applySetting("hibernate.log_session_metrics", "false")
  .build().let { MetadataSources(it) }

private var factory = metadataBuilder.buildMetadata().buildSessionFactory()

class SpecificationReader<T: Any>(
  private val clazz: Class<T>,
) {
  init {
    if (metadataBuilder.annotatedClasses.contains(clazz).not()) {
      metadataBuilder = metadataBuilder.addAnnotatedClass(clazz)
      factory = metadataBuilder.buildMetadata().buildSessionFactory()
    }
  }

  fun <S: T> specToSql(spec: Specification<S>) = specToSql(spec::toPredicate)

  fun <S: T> specToSql(updateSpecification: UpdateSpecification<S>) = updateToSql(updateSpecification::toPredicate)

  fun <S: T> specToSql(deleteSpecification: DeleteSpecification<S>) = deleteToSql(deleteSpecification::toPredicate)

  fun <S: T> specToSql(example: Example<S>) = specToSql {
    root, _, criteriaBuilder ->
    QueryByExamplePredicateBuilder.getPredicate(root, criteriaBuilder, example, EscapeCharacter.DEFAULT)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <S: T> deleteToSql(
    toRemove: (Root<S>, CriteriaDelete<S>, CriteriaBuilder) -> Predicate?
  ): SpecificationContext = try {
    logger.level = Level.OFF
    factory.openSession().use { session ->
      val criteriaBuilder = session.criteriaBuilder
      val delete = criteriaBuilder.createCriteriaDelete(clazz) as CriteriaDelete<S>

      toRemove(delete.root, delete, criteriaBuilder)?.let { delete.where(it) }

      extractMutationSql(delete, session)
    }
  } finally {
    logger.level = level
  }

  @Suppress("UNCHECKED_CAST")
  private fun <S: T> updateToSql(
    toUpdate: (Root<S>, CriteriaUpdate<S>, CriteriaBuilder) -> Predicate?
  ): SpecificationContext = try {
    logger.level = Level.OFF
    factory.openSession().use { session ->
      val criteriaBuilder = session.criteriaBuilder
      val update = criteriaBuilder.createCriteriaUpdate(clazz) as JpaCriteriaUpdate<S>

      toUpdate(update.root, update, criteriaBuilder)?.let { update.where(it) }

      extractMutationSql(update, session)
    }
  } finally {
    logger.level = level
  }


  @Suppress("UNCHECKED_CAST")
  private fun <S: T> specToSql(toPredicate: (Root<S>, JpaCriteriaQuery<S>, CriteriaBuilder) -> Predicate?): SpecificationContext = try {
    logger.level = Level.OFF
    return factory.openSession().use { session ->
      val criteriaBuilder = session.criteriaBuilder
      val query = criteriaBuilder.createQuery(clazz)
      toPredicate(query.from(clazz) as Root<S>, query as JpaCriteriaQuery<S>, criteriaBuilder)?.let { query.where(it) }
      extractNativeSql(session.createQuery(query), session)
    }
  } finally {
    logger.level = level
  }

  @Suppress("UNCHECKED_CAST")
  private fun <S: T> extractNativeSql(query: Query<S?>, session: Session): SpecificationContext {
    val queryImplementor = query.unwrap(SqmQueryImplementor::class.java)
    val sessionFactory = session.sessionFactory as SessionFactoryImplementor
    val statement = (queryImplementor as SqmQueryImplementor).sqmStatement
    val translator = sessionFactory.queryEngine.sqmTranslatorFactory.createSelectTranslator(
      statement as SqmSelectStatement, queryImplementor.queryOptions,
      DomainParameterXref.from(statement),
      queryImplementor.parameterBindings, queryImplementor.session.loadQueryInfluencers,
      sessionFactory.sqlTranslationEngine, true
    )
    val translate = translator.translate() as SqmTranslation<SelectStatement>

    val jdbcOperation = sessionFactory.jdbcServices.dialect.sqlAstTranslatorFactory.buildSelectTranslator(
      sessionFactory, translate.sqlAst
    ).translate(null, query.queryOptions)

    return createContext(jdbcOperation, translate)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <S: T> extractMutationSql(
    mutation: CriteriaUpdate<S>,
    session: Session
  ): SpecificationContext {
    return extractMutationSql(session.createMutationQuery(mutation), session)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <S: T> extractMutationSql(
    mutation: CriteriaDelete<S>,
    session: Session
  ): SpecificationContext {
    return extractMutationSql(session.createMutationQuery(mutation), session)
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractMutationSql(
    mutationQuery: MutationQuery,
    session: Session
  ): SpecificationContext {
    val queryImplementor = mutationQuery as SqmQueryImplementor<T>
    val sessionFactory = session.sessionFactory as SessionFactoryImplementor
    val statement = queryImplementor.sqmStatement as SqmDmlStatement<*>

    val translator = sessionFactory.queryEngine.sqmTranslatorFactory.createMutationTranslator(
      statement,
      queryImplementor.queryOptions,
      DomainParameterXref.from(statement as SqmStatement<*>),
      queryImplementor.parameterBindings,
      queryImplementor.session.loadQueryInfluencers,
      sessionFactory.sqlTranslationEngine
    )

    val translate = translator.translate()

    val jdbcOperation = sessionFactory.jdbcServices.dialect.sqlAstTranslatorFactory
      .buildMutationTranslator(sessionFactory, translate.sqlAst)
      .translate(null, queryImplementor.queryOptions)
    return createContext(jdbcOperation, translate)
  }

  private fun <St: Statement> createContext(
    jdbcOperation: JdbcOperation,
    translate: SqmTranslation<St>
  ): SpecificationContext {
    val parameters = jdbcOperation.parameterBinders.mapNotNull { binder ->
      translate.jdbcParamsBySqmParam.entries.find { (_, param) ->
        param[0][0].parameterBinder == binder
      }?.let { (key, _) ->
        SpecificationParameter(key.toHqlString(), key.parameterType as Class<Any>)
      }
    }
    return SpecificationContext(jdbcOperation.sqlString, parameters)
  }
}
