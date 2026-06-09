package pmeig.spring.libraries.jpa.core.executor.model.jpa

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

@Suppress("UNCHECKED_CAST")
class MethodSpecificationFactory(
  section: String
) {

  private var useArgument = true
  private val specificationFactory: (Any?) -> Specification<Any>
  = section.split("Or", ignoreCase = true).map { transformToSpecificationFactory(it) }
    .reduce { acc, specification ->
      {
        acc(it).or(specification(it))
      }
    }

  fun factory(args: MutableList<Any?>): Specification<Any> {
    return specificationFactory(if (useArgument) args.removeAt(0) else args.firstOrNull())
  }



  private fun transformToSpecificationFactory(treatment: String): (Any?) -> Specification<Any> {
    val fieldExpression = getColumn(treatment)
    val toPredicate: (CriteriaBuilder, Root<Any>, Any?) -> Predicate = when {
      treatment.endsWith("Not", ignoreCase = true) || treatment.endsWith("NotEqual", ignoreCase = true) -> { builder, root, arg ->  builder.notEqual(fieldExpression(root), arg) }
      treatment.endsWith("NotLike", ignoreCase = true) -> { builder, root, arg -> builder.notLike(fieldExpression(root) as Expression<String>, arg as String) }
      treatment.endsWith("isNotNull", ignoreCase = true) -> {
        useArgument = false
        { builder, root, _ -> builder.isNotNull(fieldExpression(root)) }
      }
      treatment.endsWith("IsNull", ignoreCase = true) -> {
        useArgument = false
        { builder, root, _ -> builder.isNull(fieldExpression(root)) }
      }
      treatment.endsWith("Like", ignoreCase = true) ->
        { builder, root, arg -> builder.like(fieldExpression(root) as Expression<String>, arg as String) }
      treatment.endsWith("In", ignoreCase = true) -> { _, root, arg -> fieldExpression(root).`in`(arg as Collection<Any>) }
      else -> { builder, root, arg -> builder.equal(fieldExpression(root), arg) }
    }
    return {
      Specification { root, _, builder -> toPredicate(builder, root, it) }
    }
  }

  private fun getColumn(treatment: String): (Root<Any>) -> Expression<Any> {
    val deep = treatment.split("_").map {
      it[0].lowercase() + it.substring(1)
    }
    return { root -> deep.fold(root as Path<Any>) { acc, part -> acc[part] } }
  }

}