package pmeig.spring.libraries.jpa.data.bigquery.converter

import com.google.cloud.bigquery.TableResult
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.data.bigquery.manager.BigQueryEntityManagerService

@Component
class BigQueryTableResultConverter(
  private val entityManagerService: BigQueryEntityManagerService,
  private val entityConverter: BigQueryEntityConverter
): Converter<TableResult, List<Map<String, Any?>>> {
  override fun convert(source: TableResult): List<Map<String, Any?>> {
    val metadata = entityConverter.convert(mapOf<String, Any>())
    val mappers = entityManagerService.getMappers(source.schema,metadata)
    return source.iterateAll().map {
      mappers.entries.associate { (key, mapper) -> key to mapper.map(it.get(key)) }
    }
  }
}