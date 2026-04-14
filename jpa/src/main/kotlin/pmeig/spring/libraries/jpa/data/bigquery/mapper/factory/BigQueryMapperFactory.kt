package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryDate
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryObjectMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryPrimitive
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Component
class BigQueryMapperFactory(
  private val jsonMapper: ObjectMapper
) {

  fun factory(field: Field, metadata: BigQueryMetadataFactory = BigQueryMetadataFactory()): BigQueryMapper<*> {
    val type = field.type.standardType
    if (field.mode == Field.Mode.REPEATED) {
      val valueMapper = factoryWithoutArray(field, type, metadata, { it.subType })
      return BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY, metadata.type)!!
        .factory(jsonMapper, valueMapper, mapOf())
    }
    return factoryWithoutArray(field, type, metadata, { it.type })
  }

  fun fromSchema(
    schema: Schema?,
    metadata: Map<String, BigQueryMetadataFactory> = emptyMap()
  ): Map<String, BigQueryMapper<*>> {
    return schema?.fields?.associate {
      it.name to factory(it, metadata[it.name] ?: BigQueryMetadataFactory())
    } ?: emptyMap()
  }

  fun toMetadataFactory(type: Type): BigQueryMetadataFactory {
    if (type is ParameterizedType && !(type.rawType as Class<*>).isAssignableFrom(Map::class.java)) {
      return BigQueryMetadataFactory(
        type.rawType,
        type.actualTypeArguments.first()
      )
    }
    val clazz = type as? Class<*> ?: (type as? ParameterizedType)?.rawType as? Class<*>
    ?: error("Cannot extract type from ${type.typeName}")
    if (clazz.isAssignableFrom(Map::class.java)) {
      val valueType = (type as ParameterizedType).actualTypeArguments.last()
      return BigQueryMetadataFactory(
        clazz, null, if (valueType.typeName == Any::class.qualifiedName)
          BigQueryStructType() else
          BigQueryStructType(
            emptyMap(), toMetadataFactory(valueType)
          )
      )
    }
    return BigQueryMetadataFactory(type)
  }

  fun toMetadataFactory(value: FieldAccessor<*>): BigQueryMetadataFactory =
    if (value.struct.isNotEmpty())
      BigQueryMetadataFactory(
        Map::class.java,
        null,
        toStructMetadata(value.struct)
      )
    else
      toMetadataFactory(value.type)

  private fun toStructMetadata(structFields: Map<String, FieldAccessor<*>>) =
    BigQueryStructType(structFields.mapValues { toMetadataFactory(it.value) })

  private fun factoryWithoutArray(
    field: Field,
    type: StandardSQLTypeName,
    metadata: BigQueryMetadataFactory,
    getType: (BigQueryMetadataFactory) -> Type?
  ): BigQueryMapper<*> {
    if (type == StandardSQLTypeName.STRUCT) {
      val struct = field.subFields
        .associate {
          val name = it.name
          name to factory(
            it, metadata.structType.columns[name]
              ?: metadata.structType.all ?: BigQueryMetadataFactory()
          )
        }
      return BigQueryObjectMapper.from(StandardSQLTypeName.STRUCT, metadata.type)!!.factory(jsonMapper, null, struct)
    }
    return BigQueryObjectMapper.from(type, metadata.type)?.factory(jsonMapper, null, mapOf())
      ?: provideNoBigQueryObjectMapper(type, getType(metadata))
      ?: error("No mapper found for type $type with metadata $metadata")
  }

  private fun provideNoBigQueryObjectMapper(type: StandardSQLTypeName, target: Type? = null): BigQueryMapper<*>? {
    return (BigQueryPrimitive.from(type, target) ?: BigQueryDate.from(type, target))?.mapper
  }
}