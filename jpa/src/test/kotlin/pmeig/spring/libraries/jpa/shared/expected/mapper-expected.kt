package pmeig.spring.libraries.jpa.shared.expected

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.StandardSQLTypeName
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryDate
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryObjectMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryPrimitive
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperProvider
import java.lang.reflect.Type
import kotlin.reflect.KClass

private val cache = mutableMapOf<String, BigQueryMapper<Any>>()

fun mapper_primitive_expected(type: StandardSQLTypeName, target: Type) = getMapper(
  type,
  target
) {
  BigQueryPrimitive.from(type, target)!!
}

fun mapper_primitive_expected(type: StandardSQLTypeName, target: KClass<*>) = mapper_primitive_expected(type, target.javaObjectType)

fun mapper_date_expected(type: StandardSQLTypeName, target: KClass<*>) = getMapper(
  type,
  target
) {
  BigQueryDate.from(type, target)!!
}

@Suppress("UNCHECKED_CAST")
fun mapper_object_expected(type: StandardSQLTypeName, target: KClass<*>,
                           jsonMapper: ObjectMapper, mapper: BigQueryMapper<*>? = null,
                           struct: Map<String, BigQueryMapper<*>> = mapOf()) = getCache(type, target.javaObjectType) {
  BigQueryObjectMapper.from(type, target)!!.factory(jsonMapper, mapper, struct) as BigQueryMapper<Any>
}

private fun getMapper(type: StandardSQLTypeName, target: KClass<*>, creator: () -> BigQueryMapperProvider): BigQueryMapper<Any>
  = getMapper(type, target.javaObjectType, creator)


@Suppress("UNCHECKED_CAST")
private fun getMapper(type: StandardSQLTypeName, target: Type, creator: () -> BigQueryMapperProvider): BigQueryMapper<Any>
  = getCache(type, target) {
  creator().mapper as BigQueryMapper<Any>
}

private fun getCache(type: StandardSQLTypeName, target: Type, creator: () -> BigQueryMapper<Any>): BigQueryMapper<Any>
= cache.getOrPut("$type-${target.typeName}") {
  creator()
}
