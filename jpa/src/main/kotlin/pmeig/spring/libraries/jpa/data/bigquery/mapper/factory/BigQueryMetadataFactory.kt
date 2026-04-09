package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import java.lang.reflect.Type

data class BigQueryStructType(
  val columns: Map<String, BigQueryMetadataFactory> = emptyMap(),
  val all: BigQueryMetadataFactory? = null
)

data class BigQueryMetadataFactory(
  val type: Type? = null,
  val subType: Type? = null,
  val structType: BigQueryStructType = BigQueryStructType(),
)