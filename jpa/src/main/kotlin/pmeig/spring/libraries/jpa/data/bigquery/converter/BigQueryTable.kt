package pmeig.spring.libraries.jpa.data.bigquery.converter

import com.google.cloud.bigquery.TableDefinition
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper

class BigQueryTable(
  val definition: TableDefinition,
  mapper: Map<String, BigQueryMapper<*>>
) {
  var mapper: Map<String, BigQueryMapper<*>> = mapper
    internal set
}