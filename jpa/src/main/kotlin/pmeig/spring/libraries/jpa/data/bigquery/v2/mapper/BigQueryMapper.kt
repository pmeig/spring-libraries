package pmeig.spring.libraries.jpa.data.bigquery.v2.mapper

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryParameterValue

interface BigQueryMapper<T> {
  fun map(value: FieldValue?): T?
  fun parameter(value: Any?): QueryParameterValue?
}

