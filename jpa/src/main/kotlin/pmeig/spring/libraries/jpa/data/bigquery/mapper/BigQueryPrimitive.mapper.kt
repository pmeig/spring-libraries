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

private class BigQueryStringMapper : BigQueryMapper<String> {
  override fun map(value: FieldValue?): String? = value?.stringValue

  override fun parameter(value: Any?): QueryParameterValue? = value?.let { QueryParameterValue.string(value.toString()) }
}

private class BigQueryLongMapper : BigQueryMapper<Long> {
  override fun map(value: FieldValue?): Long? = value?.longValue

  override fun parameter(value: Any?): QueryParameterValue? = convertToNumber(value, String::toLong)?.let { QueryParameterValue.int64(it) }
}

private class BigQueryDoubleMapper : BigQueryMapper<Double> {
  override fun map(value: FieldValue?): Double? = value?.doubleValue

  override fun parameter(value: Any?): QueryParameterValue? = convertToNumber(value, String::toDouble)?.let {
    QueryParameterValue.float64(it)
  }
}

private class BigQueryBooleanMapper : BigQueryMapper<Boolean> {
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

internal enum class BigQueryPrimitive(private val type: StandardSQLTypeName,
                                     private val target: Type,
                                     override val mapper: BigQueryMapper<*>): BigQueryMapperProvider {
  STRING(StandardSQLTypeName.STRING, String::class, BigQueryStringMapper()),
  INT64(StandardSQLTypeName.INT64, Long::class, BigQueryLongMapper()),
  FLOAT64(StandardSQLTypeName.FLOAT64, Double::class, BigQueryDoubleMapper()),
  BOOL(StandardSQLTypeName.BOOL, Boolean::class, BigQueryBooleanMapper()),
  BYTES(StandardSQLTypeName.BYTES, ByteArray::class, BigQueryByteArrayMapper()),
  BIG_NUMERIC(StandardSQLTypeName.NUMERIC, BigDecimal::class, BigQueryBigDecimal()),
  NUMERIC(StandardSQLTypeName.NUMERIC, BigInteger::class, BigQueryBigDecimal());

  companion object {
    fun from(type: StandardSQLTypeName, target: Type? = null): BigQueryPrimitive? =
      target?.let { clazz -> entries.find { it.type == type && it.target.typeName == clazz.typeName } } ?: entries.find { it.type == type }
  }

  constructor(type: StandardSQLTypeName, target: KClass<*>, mapper: BigQueryMapper<*>): this(type, target.javaObjectType, mapper)
}

