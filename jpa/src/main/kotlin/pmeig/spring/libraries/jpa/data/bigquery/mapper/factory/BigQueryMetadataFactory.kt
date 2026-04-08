package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import kotlin.reflect.KClass

data class BigQueryMetadataFactory(
  val type: KClass<*>? = null,
  val subType: KClass<*>? = null,
  val structType: Map<String, BigQueryMetadataFactory> = mapOf(),
)