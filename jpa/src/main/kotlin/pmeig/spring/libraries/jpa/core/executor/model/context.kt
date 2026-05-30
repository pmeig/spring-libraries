package pmeig.spring.libraries.jpa.core.executor.model

data class QueryContext(
  val sql: String,
  val context: MethodContext = MethodContext(),
)

data class MethodContext(
  val parameters: List<ParameterContext> = emptyList(),
  val json: Boolean = false,
  val map: Boolean = false,
  val batch: Boolean = false,
  val collection: Boolean = false,
)