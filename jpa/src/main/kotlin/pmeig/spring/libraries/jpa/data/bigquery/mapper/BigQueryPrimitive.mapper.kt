package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.StandardSQLTypeName
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperProvider
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

private fun <T: Number> convertToNumber(value: Any?, mapper: (String) -> T): T? = value?.let {
  try {
    mapper(value.toString())
  } catch (_: Throwable) {
    null
  }
}

private class BigQueryStringMapper: BigQueryMapper<String> {
  override fun map(value: FieldValue?): String? = value?.stringValue

  override fun parameter(value: Any?): QueryParameterValue? =
    value?.let { QueryParameterValue.string(value.toString()) }
}

private abstract class BigQueryNumberMapper<T>(
  private val mapper: (Long) -> T
): BigQueryMapper<T> {
  override fun map(value: FieldValue?): T? = value?.longValue?.let {
    mapper(it)
  }

  override fun parameter(value: Any?): QueryParameterValue? =
    convertToNumber(value, String::toLong)?.let { QueryParameterValue.int64(it) }
}

private class BigQueryLongMapper: BigQueryNumberMapper<Long>({ it })
private class BigQueryIntMapper: BigQueryNumberMapper<Int>({ it.toInt() })
private class BigQueryShortMapper: BigQueryNumberMapper<Short>({ it.toShort() })

private class BigQueryDoubleMapper: BigQueryMapper<Double> {
  override fun map(value: FieldValue?): Double? = value?.doubleValue

  override fun parameter(value: Any?): QueryParameterValue? = convertToNumber(value, String::toDouble)?.let {
    QueryParameterValue.float64(it)
  }
}

private class BigQueryBooleanMapper: BigQueryMapper<Boolean> {
  override fun map(value: FieldValue?): Boolean? = value?.booleanValue
  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      value.toString().lowercase().toBooleanStrictOrNull()
    }?.let { QueryParameterValue.bool(it) }
  }
}

private class BigQueryBigDecimal: BigQueryMapper<BigDecimal> {
  override fun map(value: FieldValue?): BigDecimal? = value?.numericValue

  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      if (it is BigInteger) {
        it.toBigDecimal()
      } else it as? BigDecimal
    }?.let { QueryParameterValue.numeric(it) }
  }
}

private class BigQueryByteArrayMapper: BigQueryMapper<ByteArray> {
  override fun map(value: FieldValue?): ByteArray? = value?.bytesValue
  override fun parameter(value: Any?): QueryParameterValue? = value?.let {
    it as? ByteArray
  }?.let { QueryParameterValue.bytes(it) }
}

@Suppress("unused")
internal enum class BigQueryPrimitive(
  private val type: StandardSQLTypeName,
  target: List<Type>,
  override val mapper: BigQueryMapper<*>
): BigQueryMapperProvider {
  STRING(StandardSQLTypeName.STRING, String::class, BigQueryStringMapper()),
  INT64(StandardSQLTypeName.INT64, Long::class, BigQueryLongMapper()),
  INT64_INTEGER(StandardSQLTypeName.INT64, Int::class, BigQueryIntMapper()),
  INT64_SHORT(StandardSQLTypeName.INT64, Short::class, BigQueryShortMapper()),
  FLOAT64(StandardSQLTypeName.FLOAT64, Double::class, BigQueryDoubleMapper()),
  BOOL(StandardSQLTypeName.BOOL, Boolean::class, BigQueryBooleanMapper()),
  BYTES(StandardSQLTypeName.BYTES, ByteArray::class, BigQueryByteArrayMapper()),
  BIG_NUMERIC(StandardSQLTypeName.NUMERIC, BigDecimal::class, BigQueryBigDecimal()),
  NUMERIC(StandardSQLTypeName.NUMERIC, BigInteger::class, BigQueryBigDecimal());

  companion object {
    fun from(type: StandardSQLTypeName, target: KClass<*>): BigQueryPrimitive? = from(type, target.javaObjectType)
    fun from(type: StandardSQLTypeName, target: Type? = null): BigQueryPrimitive? =
      target?.let { clazz -> entries.find { it.type == type && clazz.typeName in it.targets } }
        ?: entries.find { it.type == type }

    fun from(target: Type): BigQueryPrimitive? = entries.find { it.targets.contains(target.typeName) }
  }

  private val targets = target.map { it.typeName }

  constructor(type: StandardSQLTypeName, target: KClass<*>, mapper: BigQueryMapper<*>): this(
    type,
    listOfNotNull(target.javaObjectType, target.javaPrimitiveType), mapper
  )

  constructor(type: StandardSQLTypeName, target: Class<*>, mapper: BigQueryMapper<*>): this(
    type,
    listOf(target),
    mapper
  )
}

