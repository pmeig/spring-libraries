package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.google.cloud.bigquery.StandardSQLTypeName
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
class BigQueryObjectMapperTest {

  private val objectMapper = ObjectMapper()

  @Nested
  inner class FromMethod {

    @Nested
    inner class WithTypeAndKClass {
      @Test
      fun `returns matching mapper for JSON`() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.JSON, Map::class)
        assertEquals(BigQueryObjectMapper.JSON, result)
      }

      @Test
      fun `returns matching mapper for GEOGRAPHY`() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.GEOGRAPHY, String::class)
        assertEquals(BigQueryObjectMapper.GEOGRAPHY, result)
      }

      @Test
      fun `returns matching mapper for STRUCT`() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.STRUCT, Any::class)
        assertEquals(BigQueryObjectMapper.STRUCT, result)
      }
    }

    @Nested
    inner class WithTypeAndType {
      @Test
      fun `returns matching mapper for List`() {
        val listType = object : ParameterizedType {
          override fun getRawType(): Type = List::class.java
          override fun getOwnerType(): Type? = null
          override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }

        val result = BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY, listType)
        assertEquals(BigQueryObjectMapper.LIST, result)
      }

      @Test
      fun `returns matching mapper for Set`() {
        val setType = object : ParameterizedType {
          override fun getRawType(): Type = Set::class.java
          override fun getOwnerType(): Type? = null
          override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }

        val result = BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY, setType)
        assertEquals(BigQueryObjectMapper.SET, result)
      }

      @Test
      fun `returns null when no match found`() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.INT64)
        assertNull(result)
      }
    }

    @Nested
    inner class WithTypeOnly {
      @Test
      fun `returns first matching enum entry for ARRAY`() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY)
        assertNotNull(result)
        assertTrue(result in listOf(BigQueryObjectMapper.LIST, BigQueryObjectMapper.SET, BigQueryObjectMapper.ARRAY))
      }

      @Test
      fun `returns GEOGRAPHY mapper`() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.GEOGRAPHY)
        assertEquals(BigQueryObjectMapper.GEOGRAPHY, result)
      }

      @Test
      fun `returns STRUCT mapper`() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.STRUCT)
        assertEquals(BigQueryObjectMapper.STRUCT, result)
      }
    }

    @Nested
    inner class WithTypeAlone {
      @Test
      fun `returns matching mapper for List type`() {
        val result = BigQueryObjectMapper.from(List::class.java)
        assertEquals(BigQueryObjectMapper.LIST, result)
      }

      @Test
      fun `returns null when no match`() {
        val result = BigQueryObjectMapper.from(Int::class.java)
        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryJsonMapper {

    private lateinit var mapper: BigQueryMapper<Map<String, Any?>>

    @BeforeEach
    fun setUp() {
      mapper = BigQueryObjectMapper.JSON.factory(objectMapper, null, emptyMap()) as BigQueryMapper<Map<String, Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `converts JSON string to Map`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("""{"key":"value","number":42}""")

        val result = mapper.map(fieldValue)

        assertNotNull(result)
        assertEquals("value", result!!["key"])
        assertEquals(42, result["number"])
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts JsonObject to QueryParameterValue`() {
        val jsonObject = JsonObject()
        jsonObject.addProperty("key", "value")

        val result = mapper.parameter(jsonObject)
        assertNotNull(result)
      }

      @Test
      fun `converts Map to JSON string QueryParameterValue`() {
        val map = mapOf("key" to "value", "number" to 42)

        val result = mapper.parameter(map)
        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryListMapper {

    private lateinit var itemMapper: BigQueryMapper<String>
    private lateinit var listMapper: BigQueryMapper<MutableList<Any?>>

    @BeforeEach
    fun setUp() {
      itemMapper = mock()
      listMapper = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap()) as BigQueryMapper<MutableList<Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(listMapper.map(null))
      }

      @Test
      fun `returns null when repeatedValue is null`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(null)

        assertNull(listMapper.map(fieldValue))
      }

      @Test
      fun `converts repeated values to list`() {
        val item1 = mock<FieldValue>()
        val item2 = mock<FieldValue>()
        whenever(item1.isNull).thenReturn(false)
        whenever(item2.isNull).thenReturn(false)
        whenever(itemMapper.map(item1)).thenReturn("value1")
        whenever(itemMapper.map(item2)).thenReturn("value2")

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(listOf(item1, item2))

        val result = listMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertEquals("value1", result[0])
        assertEquals("value2", result[1])
      }

      @Test
      fun `skips null items in repeated values`() {
        val item1 = mock<FieldValue>()
        val item2 = mock<FieldValue>()
        whenever(item1.isNull).thenReturn(true)
        whenever(item2.isNull).thenReturn(false)
        whenever(itemMapper.map(null)).thenReturn(null)
        whenever(itemMapper.map(item2)).thenReturn("value2")

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(listOf(item1, item2))

        val result = listMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals("value2", result[0])
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(listMapper.parameter(null))
      }

      @Test
      fun `returns null when empty collection`() {
        val collection = emptyList<String>()

        val result = listMapper.parameter(collection)
        assertNull(result)
      }

      @Test
      fun `converts non-empty Collection to array parameter`() {
        val collection = listOf("item1", "item2")

        val result = listMapper.parameter(collection)
        assertNotNull(result)
      }

      @Test
      fun `converts non-empty Array to array parameter`() {
        val array = arrayOf("item1", "item2")

        val result = listMapper.parameter(array)
        assertNotNull(result)
      }

      @Test
      fun `returns null when empty array`() {
        val array = emptyArray<String>()

        val result = listMapper.parameter(array)
        assertNull(result)
      }

      @Test
      fun `converts Iterable to array parameter`() {
        val iterable = sequenceOf("item1", "item2").asIterable()

        val result = listMapper.parameter(iterable)
        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQuerySetMapper {

    private lateinit var itemMapper: BigQueryMapper<String>
    private lateinit var setMapper: BigQueryMapper<MutableSet<Any?>>

    @BeforeEach
    fun setUp() {
      itemMapper = mock()
      setMapper = BigQueryObjectMapper.SET.factory(objectMapper, itemMapper, emptyMap()) as BigQueryMapper<MutableSet<Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(setMapper.map(null))
      }

      @Test
      fun `converts repeated values to set`() {
        val item1 = mock<FieldValue>()
        val item2 = mock<FieldValue>()
        whenever(item1.isNull).thenReturn(false)
        whenever(item2.isNull).thenReturn(false)
        whenever(itemMapper.map(item1)).thenReturn("value1")
        whenever(itemMapper.map(item2)).thenReturn("value2")

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(listOf(item1, item2))

        val result = setMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertTrue(result.contains("value1"))
        assertTrue(result.contains("value2"))
      }

      @Test
      fun `removes duplicates in set`() {
        val item1 = mock<FieldValue>()
        val item2 = mock<FieldValue>()
        whenever(item1.isNull).thenReturn(false)
        whenever(item2.isNull).thenReturn(false)
        whenever(itemMapper.map(item1)).thenReturn("value")
        whenever(itemMapper.map(item2)).thenReturn("value")

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(listOf(item1, item2))

        val result = setMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertTrue(result.contains("value"))
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(setMapper.parameter(null))
      }

      @Test
      fun `returns null when empty set`() {
        val set = emptySet<String>()

        val result = setMapper.parameter(set)
        assertNull(result)
      }

      @Test
      fun `converts non-empty Set to array parameter`() {
        val set = setOf("item1", "item2")

        val result = setMapper.parameter(set)
        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryArrayMapper {

    private lateinit var itemMapper: BigQueryMapper<String>
    private lateinit var arrayMapper: BigQueryMapper<Array<Any?>>

    @BeforeEach
    fun setUp() {
      itemMapper = mock()
      arrayMapper = BigQueryObjectMapper.ARRAY.factory(objectMapper, itemMapper, emptyMap()) as BigQueryMapper<Array<Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(arrayMapper.map(null))
      }

      @Test
      fun `converts repeated values to array`() {
        val item1 = mock<FieldValue>()
        val item2 = mock<FieldValue>()
        whenever(item1.isNull).thenReturn(false)
        whenever(item2.isNull).thenReturn(false)
        whenever(itemMapper.map(item1)).thenReturn("value1")
        whenever(itemMapper.map(item2)).thenReturn("value2")

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(listOf(item1, item2))

        val result = arrayMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertArrayEquals(arrayOf("value1", "value2"), result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(arrayMapper.parameter(null))
      }

      @Test
      fun `returns null when empty array`() {
        val array = emptyArray<String>()

        val result = arrayMapper.parameter(array)
        assertNull(result)
      }

      @Test
      fun `converts non-empty array to QueryParameterValue`() {
        val array = arrayOf("item1", "item2")

        val result = arrayMapper.parameter(array)
        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryGeographyMapper {

    private lateinit var mapper: BigQueryMapper<String>

    @BeforeEach
    fun setUp() {
      mapper = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap()) as BigQueryMapper<String>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts string value from FieldValue`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("POINT(1 2)")

        val result = mapper.map(fieldValue)
        assertEquals("POINT(1 2)", result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts value to geography parameter`() {
        val result = mapper.parameter("POINT(1 2)")
        assertNotNull(result)
      }
    }

    @Nested
    inner class EqualsMethod {
      @Test
      fun `returns true for same instance`() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())
        val mapper2 = mapper1

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun `returns true for different instances of same class`() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())
        val mapper2 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())

        assertEquals(mapper1, mapper2)
      }
    }

    @Nested
    inner class HashCodeMethod {
      @Test
      fun `returns same hashCode for same class instances`() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())
        val mapper2 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())

        assertEquals(mapper1.hashCode(), mapper2.hashCode())
      }
    }
  }

  @Nested
  inner class BigQueryStructMapper {

    private lateinit var fieldMapper1: BigQueryMapper<String>
    private lateinit var fieldMapper2: BigQueryMapper<Int>
    private lateinit var structMapper: BigQueryMapper<Map<String, Any?>>

    @BeforeEach
    fun setUp() {
      fieldMapper1 = mock()
      fieldMapper2 = mock()
      val mappers = mapOf<String, BigQueryMapper<*>>(
        "field1" to fieldMapper1,
        "field2" to fieldMapper2
      )
      structMapper = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers) as BigQueryMapper<Map<String, Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(structMapper.map(null))
      }

      @Test
      fun `returns null when recordValue is null`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.recordValue).thenReturn(null)

        assertNull(structMapper.map(fieldValue))
      }

      @Test
      fun `converts struct fields to map`() {
        val field1Value = mock<FieldValue>()
        val field2Value = mock<FieldValue>()
        whenever(field1Value.isNull).thenReturn(false)
        whenever(field2Value.isNull).thenReturn(false)
        whenever(fieldMapper1.map(field1Value)).thenReturn("value1")
        whenever(fieldMapper2.map(field2Value)).thenReturn(42)

        val recordValue = mock<FieldValueList>()
        whenever(recordValue.get("field1")).thenReturn(field1Value)
        whenever(recordValue.get("field2")).thenReturn(field2Value)

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.recordValue).thenReturn(recordValue)

        val result = structMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertEquals("value1", result["field1"])
        assertEquals(42, result["field2"])
      }

      @Test
      fun `handles null struct fields`() {
        val field1Value = mock<FieldValue>()
        whenever(field1Value.isNull).thenReturn(true)
        whenever(fieldMapper1.map(null)).thenReturn(null)

        val field2Value = mock<FieldValue>()
        whenever(field2Value.isNull).thenReturn(false)
        whenever(fieldMapper2.map(field2Value)).thenReturn(42)

        val recordValue = mock<FieldValueList>()
        whenever(recordValue.get("field1")).thenReturn(field1Value)
        whenever(recordValue.get("field2")).thenReturn(field2Value)

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.recordValue).thenReturn(recordValue)

        val result = structMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertNull(result["field1"])
        assertEquals(42, result["field2"])
      }
    }

    @Nested
    inner class EqualsMethod {
      @Test
      fun `returns true for same mappers`() {
        val mappers = mapOf<String, BigQueryMapper<*>>("field" to mock())
        val mapper1 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)
        val mapper2 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)

        assertEquals(mapper1, mapper2)
      }
    }

    @Nested
    inner class HashCodeMethod {
      @Test
      fun `returns same hashCode for same mappers`() {
        val mappers = mapOf<String, BigQueryMapper<*>>("field" to mock())
        val mapper1 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)
        val mapper2 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)

        assertEquals(mapper1.hashCode(), mapper2.hashCode())
      }
    }
  }

  @Nested
  inner class FactoryMethod {

    @Nested
    inner class Initialization {
      @Test
      fun `JSON factory is initialized once`() {
        val mapper1 = BigQueryObjectMapper.JSON.factory(objectMapper, null, emptyMap())
        val mapper2 = BigQueryObjectMapper.JSON.factory(objectMapper, null, emptyMap())

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun `GEOGRAPHY factory is initialized once`() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())
        val mapper2 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun `LIST factory creates equivalent instances`() {
        val itemMapper = mock<BigQueryMapper<*>>()
        val mapper1 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())
        val mapper2 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())

        assertEquals(mapper1, mapper2)
      }
    }
  }

  @Nested
  inner class EqualsAndHashCode {

    @Test
    fun `BigQueryArrayMapper equals returns true for same item mapper`() {
      val itemMapper = mock<BigQueryMapper<*>>()
      val mapper1 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())
      val mapper2 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())

      assertEquals(mapper1, mapper2)
      assertEquals(mapper1.hashCode(), mapper2.hashCode())
    }
  }
}