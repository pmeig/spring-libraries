package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.google.cloud.bigquery.FieldValue
import pmeig.spring.libraries.jpa.core.FieldAccessor

class BigQueryFieldMapper<T>(
  private val accessor: FieldAccessor<T>,
  private val mapper: BigQueryMapper<T>
) {

  fun map(entity: Any, value: FieldValue): Any {
    return accessor.set(entity, mapper.map(value))
  }
}