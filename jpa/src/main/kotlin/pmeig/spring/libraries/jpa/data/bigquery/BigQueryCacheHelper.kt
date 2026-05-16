package pmeig.spring.libraries.jpa.data.bigquery

import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper

const val BIG_QUERY_MAPPER_NAME = "data-fields-mapper"

@Suppress("UNCHECKED_CAST")
fun mappers(cacheManager: DataCacheManager, metadata: DataMetadata, compute: () -> Map<String, BigQueryFieldMapper<*>>)
: Map<String, BigQueryFieldMapper<*>>
= DataCacheNames.useCache(
  cacheManager, BIG_QUERY_MAPPER_NAME, metadata.reference.typeName + "_"
          + metadata.columns.keys.joinToString("-"), Map::class.java, compute
) as Map<String, BigQueryFieldMapper<*>>