package pmeig.spring.libraries.jpa.data.bigquery.converter

import org.hibernate.mapping.Collection
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import pmeig.spring.libraries.jpa.core.TypeCombineReference
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalAmount

@Component
class BigQueryTypeConverter: Converter<Type, BigQueryMetadataFactory> {
  override fun convert(source: Type): BigQueryMetadataFactory {
    var type: Type = source
    val clazz = Class.forName(type.typeName)
    if (ClassUtils.isAssignable(clazz, Collection::class.java)
      || ClassUtils.isAssignable(clazz, Iterable::class.java)
    ) {
      type = TypeCombineReference(type as ParameterizedType, Array::class)
    }
    if ((type as ParameterizedType).rawType.typeName == Array::class.java.typeName) {
      return BigQueryMetadataFactory(
        (type.rawType as Class<*>).kotlin,
        (type.actualTypeArguments[0] as Class<*>).kotlin
      )
    }
    if (clazz.isPrimitive ||
      ClassUtils.isAssignable(clazz, TemporalAccessor::class.java)
      || ClassUtils.isAssignable(clazz, Number::class.java)
      || ClassUtils.isAssignable(clazz, TemporalAmount::class.java)
    ) {
      return BigQueryMetadataFactory(clazz.kotlin)
    }
    return BigQueryMetadataFactory(Map::class, null, extractFields(clazz).map {
      it.key to convert(it.value.genericType)
    }.toMap())
  }

  fun extractField(entity: Any): Map<String, Field> = extractFields(ClassUtils.getUserClass(entity))
  fun extractFields(clazz: Class<*>): Map<String, Field> {
    if (clazz == Any::class.java) return emptyMap()
    return clazz.declaredFields.associate {
      it.name to it.let { field ->
        field.isAccessible = true
        field
      }
    } + extractFields(clazz.superclass)
  }
}