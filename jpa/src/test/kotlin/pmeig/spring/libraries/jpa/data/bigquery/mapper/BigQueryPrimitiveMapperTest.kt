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
      fun should_return_STRING_mapper_when_type_is_STRING_and_target_is_String() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.STRING, String::class)
        assertEquals(BigQueryPrimitive.STRING, result)
      }

      @Test
      fun should_return_INT64_mapper_when_type_is_INT64_and_target_is_Long() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64, Long::class)
        assertEquals(BigQueryPrimitive.INT64, result)
      }

      @Test
      fun should_return_INT64_INTEGER_mapper_when_type_is_INT64_and_target_is_Int() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64, Int::class)
        assertEquals(BigQueryPrimitive.INT64_INTEGER, result)
      }

      @Test
      fun should_return_INT64_SHORT_mapper_when_type_is_INT64_and_target_is_Short() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64, Short::class)
        assertEquals(BigQueryPrimitive.INT64_SHORT, result)
      }

      @Test
      fun should_return_FLOAT64_mapper_when_type_is_FLOAT64_and_target_is_Double() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.FLOAT64, Double::class)
        assertEquals(BigQueryPrimitive.FLOAT64, result)
      }

      @Test
      fun should_return_BOOL_mapper_when_type_is_BOOL_and_target_is_Boolean() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.BOOL, Boolean::class)
        assertEquals(BigQueryPrimitive.BOOL, result)
      }

      @Test
      fun should_return_BYTES_mapper_when_type_is_BYTES_and_target_is_ByteArray() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.BYTES, ByteArray::class)
        assertEquals(BigQueryPrimitive.BYTES, result)
      }

      @Test
      fun should_return_BIG_NUMERIC_mapper_when_type_is_NUMERIC_and_target_is_BigDecimal() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.NUMERIC, BigDecimal::class)
        assertEquals(BigQueryPrimitive.BIG_NUMERIC, result)
      }

      @Test
      fun should_return_NUMERIC_mapper_when_type_is_NUMERIC_and_target_is_BigInteger() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.NUMERIC, BigInteger::class)
        assertEquals(BigQueryPrimitive.NUMERIC, result)
      }
    }

    @Nested
    inner class WithTypeAndType {
      @Test
      fun should_return_STRING_mapper_when_type_is_STRING_and_target_type_is_String() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.STRING, String::class.javaObjectType)
        assertEquals(BigQueryPrimitive.STRING, result)
      }

      @Test
      fun should_return_null_when_no_entry_matches_the_given_type() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.TIMESTAMP, String::class.javaObjectType)
        assertNull(result)
      }

      @Test
      fun should_fallback_to_mapper_matching_type_only_when_target_does_not_match_any_entry_for_that_type() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64, String::class.javaObjectType)
        assertEquals(BigQueryPrimitive.INT64, result)
      }

      @Test
      fun should_return_first_matching_mapper_when_target_is_null() {
        val result = BigQueryPrimitive.from(StandardSQLTypeName.INT64)
        assertEquals(BigQueryPrimitive.INT64, result)
      }
    }

    @Nested
    inner class WithTypeOnly {
      @Test
      fun should_return_STRING_mapper_when_target_type_is_String() {
        val result = BigQueryPrimitive.from(String::class.javaObjectType)
        assertEquals(BigQueryPrimitive.STRING, result)
      }

      @Test
      fun should_return_INT64_mapper_when_target_type_is_Long() {
        val result = BigQueryPrimitive.from(Long::class.javaObjectType)
        assertEquals(BigQueryPrimitive.INT64, result)
      }

      @Test
      fun should_return_null_when_no_entry_targets_the_given_type() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_string_value_when_field_value_is_not_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("test-string")

        val result = mapper.map(fieldValue)

        assertEquals("test-string", result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_string_to_query_parameter_value_when_value_is_a_string() {
        val result = mapper.parameter("test-string")

        assertNotNull(result)
      }

      @Test
      fun should_convert_non_string_to_string_query_parameter_value_when_value_is_not_a_string() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_long_value_when_field_value_is_not_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.longValue).thenReturn(123456789L)

        val result = mapper.map(fieldValue)

        assertEquals(123456789L, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_long_to_query_parameter_value_when_value_is_a_long() {
        val result = mapper.parameter(123456789L)

        assertNotNull(result)
      }

      @Test
      fun should_convert_string_number_to_query_parameter_value_when_value_is_a_numeric_string() {
        val result = mapper.parameter("123456789")

        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_cannot_be_converted_to_long() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_int_value_when_field_value_is_not_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.longValue).thenReturn(123456L)

        val result = mapper.map(fieldValue)

        assertEquals(123456, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_int_to_query_parameter_value_when_value_is_an_int() {
        val result = mapper.parameter(123456)

        assertNotNull(result)
      }

      @Test
      fun should_convert_string_number_to_query_parameter_value_when_value_is_a_numeric_string() {
        val result = mapper.parameter("123456")

        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_cannot_be_converted_to_int() {
        val result = mapper.parameter("not-a-number")

        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryShortMapper {

    private val mapper = BigQueryPrimitive.INT64_SHORT.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_short_value_when_field_value_is_not_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.longValue).thenReturn(1234L)

        val result = mapper.map(fieldValue)

        assertEquals(1234.toShort(), result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_short_to_query_parameter_value_when_value_is_a_short() {
        val result = mapper.parameter(1234.toShort())

        assertNotNull(result)
      }

      @Test
      fun should_convert_string_number_to_query_parameter_value_when_value_is_a_numeric_string() {
        val result = mapper.parameter("1234")

        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_cannot_be_converted_to_short() {
        val result = mapper.parameter("not-a-number")

        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryDoubleMapper {

    private val mapper = BigQueryPrimitive.FLOAT64.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_double_value_when_field_value_is_not_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.doubleValue).thenReturn(123.456)

        val result = mapper.map(fieldValue)

        assertEquals(123.456, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_double_to_query_parameter_value_when_value_is_a_double() {
        val result = mapper.parameter(123.456)

        assertNotNull(result)
      }

      @Test
      fun should_convert_string_number_to_query_parameter_value_when_value_is_a_numeric_string() {
        val result = mapper.parameter("123.456")

        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_cannot_be_converted_to_double() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_boolean_value_when_field_value_is_not_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.booleanValue).thenReturn(true)

        val result = mapper.map(fieldValue)

        assertEquals(true, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_boolean_to_query_parameter_value_when_value_is_a_boolean() {
        val result = mapper.parameter(true)

        assertNotNull(result)
      }

      @Test
      fun should_convert_string_true_to_query_parameter_value_when_value_is_the_string_true() {
        val result = mapper.parameter("true")

        assertNotNull(result)
      }

      @Test
      fun should_convert_string_false_to_query_parameter_value_when_value_is_the_string_false() {
        val result = mapper.parameter("false")

        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_cannot_be_converted_to_boolean() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_big_decimal_value_when_field_value_is_not_null() {
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
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_big_decimal_to_query_parameter_value_when_value_is_a_big_decimal() {
        val result = mapper.parameter(BigDecimal("123.456"))

        assertNotNull(result)
      }

      @Test
      fun should_convert_big_integer_to_big_decimal_query_parameter_value_when_value_is_a_big_integer() {
        val result = mapper.parameter(BigInteger("123456"))

        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_is_neither_big_decimal_nor_big_integer() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_byte_array_value_when_field_value_is_not_null() {
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
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_byte_array_to_query_parameter_value_when_value_is_a_byte_array() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        val result = mapper.parameter(bytes)

        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_is_not_a_byte_array() {
        val result = mapper.parameter("not-bytes")

        assertNull(result)
      }
    }
  }
}
