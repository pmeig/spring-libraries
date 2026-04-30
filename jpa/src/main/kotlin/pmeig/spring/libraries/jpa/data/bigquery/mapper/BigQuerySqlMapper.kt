package pmeig.spring.libraries.jpa.data.bigquery.mapper

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata

private const val ORDER_BY = " ORDER BY "
private const val LIMIT = " LIMIT "
private const val OFFSET = " OFFSET "

@Component
class BigQuerySqlMapper {

  fun pageQuery(query: String,
                  pageable: Pageable,
                  metadata: DataMetadata): String {
    val orderBy = extractOrderBy(query, metadata)
    val limit = extractLimit(query, pageable)
    val offset = extractOffset(query, pageable)
    val sqlWithoutSelector = removeSelector(query)
    return """
      WITH __count AS (SELECT COUNT(${metadata.primary.columns.keys.joinToString(",")}) as count $sqlWithoutSelector), 
      __result AS (${extractQueryWithoutOrderLimitAndOffset(sqlWithoutSelector, orderBy, limit, offset)})
      SELECT ARRAY_AGG(items) AS items, ANY_VALUE(__count.count) AS total FROM __count, __result items
    """.trimIndent()
  }

  private fun removeSelector(query: String): String {
    val index = query.indexOf(" FROM ", ignoreCase = true)
    return if (index > -1) query.substring(index) else query
  }

  private fun extractQueryWithoutOrderLimitAndOffset(
    query: String,
    vararg indexes: Pair<Int, String>,
  ): String {
    val indexEnd = takeFirstFound(indexes.map { it.first })
    return if (indexEnd > -1) query.substring(0, indexEnd) else query
  }

  private fun takeFirstFound(indexes: List<Int>): Int {
    return indexes.find {
      it > -1
    } ?: -1
  }

  private fun extractOffset(query: String, pageable: Pageable) = extractPartPageQuery(query, OFFSET, pageable.offset) { -1 }

  private fun extractLimit(query: String, pageable: Pageable) = extractPartPageQuery(query, LIMIT, pageable.pageSize, ::endIndexLimit)

  private fun extractPartPageQuery(query: String, pattern: String, value: Number, lastIndexFounder: (String) -> Int) =
    extractPartQuery(query, pattern, lastIndexFounder).let { Pair(it.first, it.second.ifEmpty { " $pattern $value " }) }

  private fun extractOrderBy(query: String, metadata: DataMetadata) = extractPartQuery(query, ORDER_BY, ::endIndexOrderBy).let {
    if (it.first == -1) Pair(-1, metadata.primary.columns.keys.joinToString(",")) else it
  }

  private fun extractPartQuery(query: String, pattern: String, lastIndexFounder: (String) -> Int): Pair<Int, String> {
    val index = query.lastIndexOf(pattern, ignoreCase = true)
    return Pair(index, if (index > -1) {
      query.substring(index, lastIndexFounder(query))
    } else "")
  }

  private fun endIndexOrderBy(query: String) = query.lastIndexOf(" LIMIT ", ignoreCase = true).let {
    if (it == -1) endIndexLimit(query) else it
  }

  private fun endIndexLimit(query: String) = query.lastIndexOf(" OFFSET ", ignoreCase = true)
}