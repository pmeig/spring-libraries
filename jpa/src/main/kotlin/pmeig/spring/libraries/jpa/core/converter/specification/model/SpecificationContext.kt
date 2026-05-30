package pmeig.spring.libraries.jpa.core.converter.specification.model

data class SpecificationContext(
  val sql: String,
  val parameters: List<SpecificationParameter>
)