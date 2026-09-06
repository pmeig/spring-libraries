@file:Suppress("unused")

package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
class BigQueryObjectMapperTest {

  private val objectMapper = ObjectMapper()

  private class StructTestTarget(@JvmField val field1: String, @JvmField val field2: Int)

  @Nested
  inner class FromMethod {

    @Nested
    inner class WithTypeAndKClass {
      @Test
      fun should_return_JSON_mapper_when_type_and_kclass_match() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.JSON, Map::class)
        assertEquals(BigQueryObjectMapper.JSON, result)
      }

      @Test
      fun should_return_GEOGRAPHY_mapper_when_type_and_kclass_match() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.GEOGRAPHY, String::class)
        assertEquals(BigQueryObjectMapper.GEOGRAPHY, result)
      }

      @Test
      fun should_return_STRUCT_mapper_when_type_and_kclass_match() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.STRUCT, Any::class)
        assertEquals(BigQueryObjectMapper.STRUCT, result)
      }
    }

    @Nested
    inner class WithTypeAndType {
      @Test
      fun should_return_LIST_mapper_when_type_is_ARRAY_and_target_is_list_type() {
        val listType = object: ParameterizedType {
          override fun getRawType(): Type = List::class.java
          override fun getOwnerType(): Type? = null
          override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }

        val result = BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY, listType)
        assertEquals(BigQueryObjectMapper.LIST, result)
      }

      @Test
      fun should_return_SET_mapper_when_type_is_ARRAY_and_target_is_set_type() {
        val setType = object: ParameterizedType {
          override fun getRawType(): Type = Set::class.java
          override fun getOwnerType(): Type? = null
          override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }

        val result = BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY, setType)
        assertEquals(BigQueryObjectMapper.SET, result)
      }

      @Test
      fun should_return_type_only_match_when_target_is_provided_but_not_assignable() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY, String::class.java)

        assertNotNull(result)
        assertTrue(result in listOf(BigQueryObjectMapper.LIST, BigQueryObjectMapper.SET, BigQueryObjectMapper.ARRAY))
      }

      @Test
      fun should_return_null_when_type_has_no_matching_entry() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.INT64)
        assertNull(result)
      }
    }

    @Nested
    inner class WithTypeOnly {
      @Test
      fun should_return_first_matching_entry_when_target_is_not_provided_for_ARRAY() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.ARRAY)
        assertNotNull(result)
        assertTrue(result in listOf(BigQueryObjectMapper.LIST, BigQueryObjectMapper.SET, BigQueryObjectMapper.ARRAY))
      }

      @Test
      fun should_return_GEOGRAPHY_mapper_when_target_is_not_provided() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.GEOGRAPHY)
        assertEquals(BigQueryObjectMapper.GEOGRAPHY, result)
      }

      @Test
      fun should_return_STRUCT_mapper_when_target_is_not_provided() {
        val result = BigQueryObjectMapper.from(StandardSQLTypeName.STRUCT)
        assertEquals(BigQueryObjectMapper.STRUCT, result)
      }
    }

    @Nested
    inner class WithTypeAlone {
      @Test
      fun should_return_LIST_mapper_when_target_type_is_assignable_to_list() {
        val result = BigQueryObjectMapper.from(List::class.java)
        assertEquals(BigQueryObjectMapper.LIST, result)
      }

      @Test
      fun should_return_null_when_no_entry_target_is_assignable_from_type() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_convert_json_string_to_map_when_field_value_has_string_value() {
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
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_json_object_to_query_parameter_value_when_value_is_json_object() {
        val jsonObject = JsonObject()
        jsonObject.addProperty("key", "value")

        val result = mapper.parameter(jsonObject)
        assertNotNull(result)
      }

      @Test
      fun should_convert_map_to_json_query_parameter_value_when_value_is_not_json_object() {
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
      listMapper =
        BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap()) as BigQueryMapper<MutableList<Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun should_return_null_when_field_value_is_null() {
        assertNull(listMapper.map(null))
      }

      @Test
      fun should_return_null_when_repeated_value_is_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(null)

        assertNull(listMapper.map(fieldValue))
      }

      @Test
      fun should_convert_repeated_values_to_list_when_items_are_not_null() {
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
      fun should_skip_null_items_when_repeated_values_contain_null() {
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
      fun should_return_null_when_value_is_null() {
        assertNull(listMapper.parameter(null))
      }

      @Test
      fun should_return_null_when_collection_is_empty() {
        val collection = emptyList<String>()

        val result = listMapper.parameter(collection)
        assertNull(result)
      }

      @Test
      fun should_convert_collection_to_array_parameter_when_collection_is_not_empty() {
        val collection = listOf("item1", "item2")

        val result = listMapper.parameter(collection)
        assertNotNull(result)
      }

      @Test
      fun should_convert_array_to_array_parameter_when_array_is_not_empty() {
        val array = arrayOf("item1", "item2")

        val result = listMapper.parameter(array)
        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_array_is_empty() {
        val array = emptyArray<String>()

        val result = listMapper.parameter(array)
        assertNull(result)
      }

      @Test
      fun should_convert_iterable_to_array_parameter_when_value_is_iterable_but_not_collection() {
        val iterable = sequenceOf("item1", "item2").asIterable()

        val result = listMapper.parameter(iterable)
        assertNotNull(result)
      }

      @Test
      fun should_return_null_when_value_is_neither_array_collection_nor_iterable() {
        val result = listMapper.parameter("not a collection")
        assertNull(result)
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
      setMapper =
        BigQueryObjectMapper.SET.factory(objectMapper, itemMapper, emptyMap()) as BigQueryMapper<MutableSet<Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun should_return_null_when_field_value_is_null() {
        assertNull(setMapper.map(null))
      }

      @Test
      fun should_return_null_when_repeated_value_is_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(null)

        assertNull(setMapper.map(fieldValue))
      }

      @Test
      fun should_convert_repeated_values_to_set_when_items_are_not_null() {
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
      fun should_remove_duplicates_when_repeated_values_contain_same_item_twice() {
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
      fun should_return_null_when_value_is_null() {
        assertNull(setMapper.parameter(null))
      }

      @Test
      fun should_return_null_when_set_is_empty() {
        val set = emptySet<String>()

        val result = setMapper.parameter(set)
        assertNull(result)
      }

      @Test
      fun should_convert_set_to_array_parameter_when_set_is_not_empty() {
        val set = setOf("item1", "item2")

        val result = setMapper.parameter(set)
        assertNotNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryTableMapper {

    private lateinit var itemMapper: BigQueryMapper<String>
    private lateinit var arrayMapper: BigQueryMapper<Array<Any?>>

    @BeforeEach
    fun setUp() {
      itemMapper = mock()
      arrayMapper =
        BigQueryObjectMapper.ARRAY.factory(objectMapper, itemMapper, emptyMap()) as BigQueryMapper<Array<Any?>>
    }

    @Nested
    inner class MapMethod {
      @Test
      fun should_return_null_when_field_value_is_null() {
        assertNull(arrayMapper.map(null))
      }

      @Test
      fun should_return_null_when_repeated_value_is_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(null)

        assertNull(arrayMapper.map(fieldValue))
      }

      @Test
      fun should_convert_repeated_values_to_array_when_items_are_not_null() {
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

      @Test
      fun should_retain_null_items_when_repeated_values_contain_null() {
        val item1 = mock<FieldValue>()
        val item2 = mock<FieldValue>()
        whenever(item1.isNull).thenReturn(true)
        whenever(item2.isNull).thenReturn(false)
        whenever(itemMapper.map(null)).thenReturn(null)
        whenever(itemMapper.map(item2)).thenReturn("value2")

        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.repeatedValue).thenReturn(listOf(item1, item2))

        val result = arrayMapper.map(fieldValue)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertNull(result[0])
        assertEquals("value2", result[1])
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(arrayMapper.parameter(null))
      }

      @Test
      fun should_return_null_when_array_is_empty() {
        val array = emptyArray<String>()

        val result = arrayMapper.parameter(array)
        assertNull(result)
      }

      @Test
      fun should_convert_array_to_query_parameter_value_when_array_is_not_empty() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(mapper.map(null))
      }

      @Test
      fun should_extract_string_value_when_field_value_is_not_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("POINT(1 2)")

        val result = mapper.map(fieldValue)
        assertEquals("POINT(1 2)", result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun should_convert_value_to_geography_parameter_when_value_is_not_null() {
        val result = mapper.parameter("POINT(1 2)")
        assertNotNull(result)
      }
    }

    @Nested
    inner class EqualsMethod {
      @Test
      fun should_return_true_when_compared_to_same_instance() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())
        val mapper2 = mapper1

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun should_return_true_when_compared_to_different_instance_of_same_class() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())
        val mapper2 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun should_return_false_when_compared_to_different_class() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())

        assertFalse(mapper1.equals("not a mapper"))
      }
    }

    @Nested
    inner class HashCodeMethod {
      @Test
      fun should_return_same_hashcode_when_instances_are_of_same_class() {
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
      fun should_return_null_when_field_value_is_null() {
        assertNull(structMapper.map(null))
      }

      @Test
      fun should_return_null_when_record_value_is_null() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.recordValue).thenReturn(null)

        assertNull(structMapper.map(fieldValue))
      }

      @Test
      fun should_convert_struct_fields_to_map_when_fields_are_not_null() {
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
      fun should_map_null_struct_field_when_field_is_null() {
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
    inner class ParameterMethod {
      @Test
      fun should_return_null_when_value_is_null() {
        assertNull(structMapper.parameter(null))
      }

      @Test
      fun should_build_struct_parameter_when_value_has_matching_fields() {
        val target = StructTestTarget("value1", 42)
        whenever(fieldMapper1.parameter("value1")).thenReturn(QueryParameterValue.string("value1"))
        whenever(fieldMapper2.parameter(42)).thenReturn(QueryParameterValue.int64(42))

        val result = structMapper.parameter(target)

        assertNotNull(result)
      }
    }

    @Nested
    inner class EqualsMethod {
      @Test
      fun should_return_true_when_compared_to_same_instance() {
        val mappers = mapOf<String, BigQueryMapper<*>>("field" to mock())
        val mapper1 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)

        assertEquals(mapper1, mapper1)
      }

      @Test
      fun should_return_true_when_mappers_are_equal() {
        val mappers = mapOf<String, BigQueryMapper<*>>("field" to mock())
        val mapper1 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)
        val mapper2 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun should_return_false_when_compared_to_different_class() {
        val mappers = mapOf<String, BigQueryMapper<*>>("field" to mock())
        val mapper1 = BigQueryObjectMapper.STRUCT.factory(objectMapper, null, mappers)

        assertFalse(mapper1.equals("not a mapper"))
      }

      @Test
      fun should_return_false_when_mappers_differ() {
        val mapper1 = BigQueryObjectMapper.STRUCT.factory(
          objectMapper, null, mapOf<String, BigQueryMapper<*>>("field" to mock())
        )
        val mapper2 = BigQueryObjectMapper.STRUCT.factory(
          objectMapper, null, mapOf<String, BigQueryMapper<*>>("other" to mock())
        )

        assertFalse(mapper1 == mapper2)
      }
    }

    @Nested
    inner class HashCodeMethod {
      @Test
      fun should_return_same_hashcode_when_mappers_are_equal() {
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
      fun should_return_same_instance_when_JSON_factory_is_called_multiple_times() {
        val mapper1 = BigQueryObjectMapper.JSON.factory(objectMapper, null, emptyMap())
        val mapper2 = BigQueryObjectMapper.JSON.factory(objectMapper, null, emptyMap())

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun should_return_same_instance_when_GEOGRAPHY_factory_is_called_multiple_times() {
        val mapper1 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())
        val mapper2 = BigQueryObjectMapper.GEOGRAPHY.factory(objectMapper, null, emptyMap())

        assertEquals(mapper1, mapper2)
      }

      @Test
      fun should_return_equal_but_distinct_instances_when_LIST_factory_is_called_multiple_times() {
        val itemMapper = mock<BigQueryMapper<*>>()
        val mapper1 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())
        val mapper2 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())

        assertEquals(mapper1, mapper2)
        assertNotSame(mapper1, mapper2)
      }
    }
  }

  @Nested
  inner class EqualsAndHashCode {

    @Test
    fun should_return_true_and_same_hashcode_when_item_mappers_are_equal() {
      val itemMapper = mock<BigQueryMapper<*>>()
      val mapper1 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())
      val mapper2 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())

      assertEquals(mapper1, mapper2)
      assertEquals(mapper1.hashCode(), mapper2.hashCode())
    }

    @Test
    fun should_return_false_when_comparing_different_array_mapper_subclasses() {
      val itemMapper = mock<BigQueryMapper<*>>()
      val listMapper = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper, emptyMap())
      val setMapper = BigQueryObjectMapper.SET.factory(objectMapper, itemMapper, emptyMap())

      assertFalse(listMapper == setMapper)
    }

    @Test
    fun should_return_false_when_item_mappers_differ() {
      val itemMapper1 = mock<BigQueryMapper<*>>()
      val itemMapper2 = mock<BigQueryMapper<*>>()
      val mapper1 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper1, emptyMap())
      val mapper2 = BigQueryObjectMapper.LIST.factory(objectMapper, itemMapper2, emptyMap())

      assertFalse(mapper1 == mapper2)
    }
  }
}
