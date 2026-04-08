package pmeig.spring.libraries.jpa.data.bigquery.v2.mapper.factory

import java.lang.reflect.Type
import kotlin.reflect.KClass

data class BigQueryMetadataFactory(
  val type: Type? = null,
  val subType: Type? = null,
  val structType: Map<String, BigQueryMetadataFactory> = mapOf(),
)