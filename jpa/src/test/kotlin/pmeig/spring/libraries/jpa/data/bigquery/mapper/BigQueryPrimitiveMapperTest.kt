package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.StandardSQLTypeName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("UNCHECKED_CAST")
class BigQueryPrimitiveMapperTest {

  @Nested
  inner class FromMethod {

    @Nested
    inner class WithTypeAndKClass {
      @Test
      fun `returns STRING mapper for String`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.STRING, String::class)
        assertEquals(BigQueryPrimitive.STRING, result)
      }

      @Test
      fun `returns INT64 mapper for Long`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64, Long::class)
        assertEquals(BigQueryPrimitive.INT64, result)
      }

      @Test
      fun `returns INT64_INTEGER mapper for Int`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64, Int::class)
        assertEquals(BigQueryPrimitive.INT64_INTEGER, result)
      }

      @Test
      fun `returns INT64_SHORT mapper for Short`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64, Short::class)
        assertEquals(BigQueryPrimitive.INT64_SHORT, result)
      }

      @Test
      fun `returns FLOAT64 mapper for Double`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.FLOAT64, Double::class)
        assertEquals(BigQueryPrimitive.FLOAT64, result)
      }

      @Test
      fun `returns BOOL mapper for Boolean`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.BOOL, Boolean::class)
        assertEquals(BigQueryPrimitive.BOOL, result)
      }

      @Test
      fun `returns BYTES mapper for ByteArray`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.BYTES, ByteArray::class)
        assertEquals(BigQueryPrimitive.BYTES, result)
      }

      @Test
      fun `returns BIG_NUMERIC mapper for BigDecimal`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.NUMERIC, BigDecimal::class)
        assertEquals(BigQueryPrimitive.BIG_NUMERIC, result)
      }

      @Test
      fun `returns NUMERIC mapper for BigInteger`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.NUMERIC, BigInteger::class)
        assertEquals(BigQueryPrimitive.NUMERIC, result)
      }
    }

    @Nested
    inner class WithTypeAndType {
      @Test
      fun `returns STRING mapper for String type`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.STRING, String::class.javaObjectType)
        assertEquals(BigQueryPrimitive.STRING, result)
      }

      @Test
      fun `returns null when no match found`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.TIMESTAMP, String::class.javaObjectType)
        assertNull(result)
      }

      @Test
      fun `returns first matching mapper when target is null`() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64)
        assertNotNull(result)
      }
    }

    @Nested
    inner class WithTypeOnly {
      @Test
      fun `returns String mapper for target type`() {
        val result = BigQueryPrimitive.from(String::class.javaObjectType)
        assertEquals(BigQueryPrimitive.STRING, result)
      }

      @Test
      fun `returns Long mapper for target type`() {
        val result = BigQueryPrimitive.from(Long::class.javaObjectType)
        assertEquals(BigQueryPrimitive.INT64, result)
      }

      @Test
      fun `returns null when no match for target type`() {
        val result = BigQueryPrimitive.from(List::class.java)
        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryStringMapper {

    private val mapper = BigQueryPrimitive.STRING.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts string value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("test-string")

        val result = mapper.map(fieldValue)

        assertEquals("test-string", result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts String to QueryParameterValue`() {
        val result = mapper.parameter("test-string")

        assertNotNull(result)
      }

      @Test
      fun `converts non-String to String QueryParameterValue`() {
        val result = mapper.parameter(123)

        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryLongMapper {

    private val mapper = BigQueryPrimitive.INT64.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts long value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.longValue).thenReturn(123456789L)

        val result = mapper.map(fieldValue)

        assertEquals(123456789L, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Long to QueryParameterValue`() {
        val result = mapper.parameter(123456789L)

        assertNotNull(result)
      }

      @Test
      fun `converts String number to QueryParameterValue`() {
        val result = mapper.parameter("123456789")

        assertNotNull(result)
      }

      @Test
      fun `returns null when value cannot be converted to Long`() {
        val result = mapper.parameter("not-a-number")

        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryIntMapper {

    private val mapper = BigQueryPrimitive.INT64_INTEGER.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts int value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.longValue).thenReturn(123456L)

        val result = mapper.map(fieldValue)

        assertEquals(123456, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Int to QueryParameterValue`() {
        val result = mapper.parameter(123456)

        assertNotNull(result)
      }

      @Test
      fun `converts String number to QueryParameterValue`() {
        val result = mapper.parameter("123456")

        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryShortMapper {

    private val mapper = BigQueryPrimitive.INT64_SHORT.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts short value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.longValue).thenReturn(1234L)

        val result = mapper.map(fieldValue)

        assertEquals(1234.toShort(), result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Short to QueryParameterValue`() {
        val result = mapper.parameter(1234.toShort())

        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryDoubleMapper {

    private val mapper = BigQueryPrimitive.FLOAT64.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts double value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.doubleValue).thenReturn(123.456)

        val result = mapper.map(fieldValue)

        assertEquals(123.456, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Double to QueryParameterValue`() {
        val result = mapper.parameter(123.456)

        assertNotNull(result)
      }

      @Test
      fun `converts String number to QueryParameterValue`() {
        val result = mapper.parameter("123.456")

        assertNotNull(result)
      }

      @Test
      fun `returns null when value cannot be converted to Double`() {
        val result = mapper.parameter("not-a-number")

        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryBooleanMapper {

    private val mapper = BigQueryPrimitive.BOOL.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts boolean value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.booleanValue).thenReturn(true)

        val result = mapper.map(fieldValue)

        assertEquals(true, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Boolean to QueryParameterValue`() {
        val result = mapper.parameter(true)

        assertNotNull(result)
      }

      @Test
      fun `converts String 'true' to QueryParameterValue`() {
        val result = mapper.parameter("true")

        assertNotNull(result)
      }

      @Test
      fun `converts String 'false' to QueryParameterValue`() {
        val result = mapper.parameter("false")

        assertNotNull(result)
      }

      @Test
      fun `returns null when value cannot be converted to Boolean`() {
        val result = mapper.parameter("not-a-boolean")

        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryBigDecimalMapper {

    private val mapper = BigQueryPrimitive.BIG_NUMERIC.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts BigDecimal value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        val expected = BigDecimal("123.456")
        whenever(fieldValue.numericValue).thenReturn(expected)

        val result = mapper.map(fieldValue)

        assertEquals(expected, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts BigDecimal to QueryParameterValue`() {
        val result = mapper.parameter(BigDecimal("123.456"))

        assertNotNull(result)
      }

      @Test
      fun `converts BigInteger to BigDecimal QueryParameterValue`() {
        val result = mapper.parameter(BigInteger("123456"))

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not BigDecimal or BigInteger`() {
        val result = mapper.parameter("not-a-number")

        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryByteArrayMapper {

    private val mapper = BigQueryPrimitive.BYTES.mapper as BigQueryMapper<ByteArray>

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts byte array value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        val expected = byteArrayOf(1, 2, 3, 4, 5)
        whenever(fieldValue.bytesValue).thenReturn(expected)

        val result = mapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(expected.toList(), result?.toList())
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts ByteArray to QueryParameterValue`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        val result = mapper.parameter(bytes)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not ByteArray`() {
        val result = mapper.parameter("not-bytes")

        assertNull(result)
      }
    }
  }
}
