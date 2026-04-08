package pmeig.spring.libraries.jpa.data.bigquery.converter

import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.BigQueryAnnotationConverter
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.BigQueryField
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQueryPrimaryKey
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQuerySQLMetadata
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import java.lang.reflect.Field
import java.lang.reflect.Type
import kotlin.reflect.KClass

@Component
class BigQueryEntityConverter(
  private val typeConverter: BigQueryTypeConverter,
  private val annotationConverter: BigQueryAnnotationConverter
) {

   fun convert(source: Any): BigQuerySQLMetadata = annotationConverter.convert(source)
  fun convert(type: KClass<*>): BigQuerySQLMetadata = annotationConverter.convert(type)
  fun convert(type: Class<*>): BigQuerySQLMetadata = annotationConverter.convert(type)
  fun convert(source: Type): BigQueryMetadataFactory = typeConverter.convert(source)
  fun extractField(entity: Any): Map<String, Field> = typeConverter.extractField(entity)
  fun extractFields(clazz: Class<*>): Map<String, Field> = typeConverter.extractFields(clazz)
  fun extractColumns(
    type: Class<*>,
    primaryKey: (BigQueryPrimaryKey) -> Unit
  ): Map<String, BigQueryField<*>> = annotationConverter.extractColumns(type, primaryKey)
}