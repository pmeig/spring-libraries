package pmeig.spring.libraries.jpa.core.entity.model

import pmeig.spring.libraries.jpa.core.accessor.FieldAccessor

data class DataMetadata(
  val table: String,
  val primary: DataPrimaryMetadata,
  val columns: Map<String, FieldAccessor<*>> = emptyMap()
)
