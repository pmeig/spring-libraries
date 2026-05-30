package pmeig.spring.libraries.jpa.core

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata

@JvmOverloads
fun createSqlPage(dataMetadata: DataMetadata, pageable: Pageable,
                  from: String = dataMetadata.table, sort: Sort = pageable.sort): String {
  return addOffset(addOrder("SELECT * FROM $from ", createDefaultSort(dataMetadata, sort)), pageable.offset)
}

fun addOrder(sql: String, sort: Sort): String {
  return sql + " ORDER BY " + sort.joinToString(",") { it.property + " " + it.direction.name.lowercase() }
}

fun addOffset(sql: String, offset: Long) = "$sql OFFSET $offset"


fun createDefaultSort(dataMetadata: DataMetadata, sort: Sort): Sort {
  if (sort.isSorted) {
    return sort
  }
  if (dataMetadata.columns.all.keys.contains("created_at")) {
    return Sort.by(Sort.Direction.DESC, "created_at")
  }
  return Sort.by(*dataMetadata.primary.fromEntityColumns.keys.toTypedArray())
}
