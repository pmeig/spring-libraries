package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryDate
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryPrimitive
import java.lang.reflect.Type

interface BigQueryMapperProvider {
  val mapper: BigQueryMapper<*>

  companion object {
    @Suppress("UNCHECKED_CAST")
    fun <T> from(type: Type): BigQueryMapperProvider = BigQueryPrimitive.from(type) as? BigQueryMapperProvider
      ?: BigQueryDate.from(type) as? BigQueryMapperProvider
      ?: throw TypeCastException("No BigQueryMapper found for type: $type")
  }
}