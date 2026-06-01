package pmeig.spring.libraries.jpa.data.bigquery

import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper
import tools.jackson.core.type.TypeReference

const val BIG_QUERY_MAPPER_NAME = "data-fields-mapper"
const val MAPPERS_CACHE = "bigquery-mappers"
const val JPA_REGISTRAR = "bigquery-jpa"

@Suppress("UNCHECKED_CAST")
fun mappers(cacheManager: DataCacheManager, metadata: DataMetadata, compute: () -> Map<String, BigQueryFieldMapper<*>>)
: Map<String, BigQueryFieldMapper<*>>
= DataCacheNames.useCache(
  cacheManager, BIG_QUERY_MAPPER_NAME, metadata.reference.typeName + "_"
          + metadata.columns.all.keys.joinToString("-"),
  object: TypeReference<Map<String, BigQueryFieldMapper<*>>>() {} , compute
)