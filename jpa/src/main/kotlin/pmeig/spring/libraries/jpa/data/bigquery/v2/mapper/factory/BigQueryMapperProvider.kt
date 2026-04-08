package pmeig.spring.libraries.jpa.data.bigquery.v2.mapper.factory

import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper

interface BigQueryMapperProvider {
  val mapper: BigQueryMapper<*>
}