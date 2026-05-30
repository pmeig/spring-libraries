package pmeig.spring.libraries.jpa.core.executor.model

import java.lang.reflect.Type

data class ParameterContext(
  var name: String,
  val type: Type,
  val position: Int
)
