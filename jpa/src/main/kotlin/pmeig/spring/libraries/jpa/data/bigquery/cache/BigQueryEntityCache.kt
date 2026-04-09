package pmeig.spring.libraries.jpa.data.bigquery.cache

import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper
import java.lang.reflect.Constructor

data class BigQueryEntityCache<T>(
  val constructor: Constructor<T>,
  val fieldMappers: Map<String, BigQueryFieldMapper<*>> = emptyMap()
) {
}