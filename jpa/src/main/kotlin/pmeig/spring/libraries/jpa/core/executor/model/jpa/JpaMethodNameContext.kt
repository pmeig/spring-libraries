package pmeig.spring.libraries.jpa.core.executor.model.jpa

import org.springframework.data.jpa.domain.Specification

data class JpaMethodNameContext(
  val toSpecification: (Array<Any?>) -> Specification<Any> = { _ -> Specification.unrestricted() },
  val postQuery: (Collection<Any?>) -> Any? = { it },
  val valid: Boolean = false
)