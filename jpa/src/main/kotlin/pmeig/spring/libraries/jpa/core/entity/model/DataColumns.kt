package pmeig.spring.libraries.jpa.core.entity.model

import pmeig.spring.libraries.jpa.core.FieldAccessor

data class DataColumns(
  val all: Map<String, FieldAccessor<Any>> = emptyMap(),
  val updated: Map<String, FieldAccessor<Any>> = emptyMap(),
) {
}