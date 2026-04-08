package pmeig.spring.libraries.jpa.data.bigquery.v2.mapper.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.core.accessor.FieldAccessor
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryDate
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryObjectMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryPrimitive
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

@Component
class BigQueryMapperFactory(
  private val jsonMapper: ObjectMapper
) {

  fun factory(field: Field, metadata: BigQueryMetadataFactory = BigQueryMetadataFactory()): BigQueryMapper<*> {
    val type = field.type.standardType
    if (field.mode == Field.Mode.REPEATED) {
      val valueMapper = provideNoBigQueryObjectMapper(type, metadata.subType)!!
      return BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY, metadata.type)!!
        .factory(jsonMapper, valueMapper, mapOf())
    }
    if (type == StandardSQLTypeName.STRUCT) {
      val struct = field.subFields
        .associate { it.name to factory(it, metadata.structType[it.name] ?: BigQueryMetadataFactory()) }
      return BigQueryObjectMapper.from(StandardSQLTypeName.STRUCT, metadata.type)!!.factory(jsonMapper, null, struct)
    }
    return BigQueryObjectMapper.from(type, metadata.type)?.factory(jsonMapper, null, mapOf())
      ?: provideNoBigQueryObjectMapper(type, metadata.subType)
      ?: error("No mapper found for type $type with metadata $metadata")
  }

  private fun provideNoBigQueryObjectMapper(type: StandardSQLTypeName, target: KClass<*>? = null): BigQueryMapper<*>? {
    return (BigQueryPrimitive.from(type, target) ?: BigQueryDate.from(type, target))?.mapper
  }

  fun fromSchema(schema: Schema?, metadata: Map<String, BigQueryMetadataFactory> = emptyMap()): Map<String, BigQueryMapper<*>> {
    return schema?.fields?.associate {
      it.name to factory(it, metadata[it.name] ?: BigQueryMetadataFactory())
    } ?: emptyMap()
  }

  fun toMetadataFactory(value: FieldAccessor<*>): BigQueryMetadataFactory {
    val type = value.type
    if (type is ParameterizedType) {
      return BigQueryMetadataFactory(
        type.rawType,
        type.actualTypeArguments.first()
      )
    }
  }
}