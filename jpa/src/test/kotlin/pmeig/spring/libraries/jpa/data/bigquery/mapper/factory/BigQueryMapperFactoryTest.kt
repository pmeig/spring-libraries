@file:Suppress("unused")

package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cache.Cache
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.data.bigquery.MAPPERS_CACHE
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BigQueryMapperFactoryTest {

  private val jsonMapper = ObjectMapper()
  private val cacheManager = mock<DataCacheManager>()
  private val cache = mock<Cache>()
  private val factory = BigQueryMapperFactory(jsonMapper, cacheManager)

  @BeforeEach
  fun setUp() {
    whenever(cacheManager.getCache(MAPPERS_CACHE)).thenReturn(cache)
    whenever(cache.get(any<String>(), any<Class<*>>())).thenReturn(null)
  }

  private fun parameterizedType(raw: Type, vararg args: Type): ParameterizedType = object : ParameterizedType {
    override fun getActualTypeArguments(): Array<Type> = arrayOf(*args)
    override fun getRawType(): Type = raw
    override fun getOwnerType(): Type? = null
  }

  private data class SimpleEntity(val id: Long = 0, val name: String = "", val active: Boolean = false)
  private data class NestedItem(val id: Long = 0, val label: String = "")
  private data class Box<T>(val value: T? = null)
  private open class BaseEntity(val id: Long = 0)
  private class ChildEntity(id: Long = 0, val name: String = "") : BaseEntity(id)

  @Nested
  inner class Factory {

    @Test
    fun should_return_mapper_when_field_is_string() {
      val field = Field.of("name", StandardSQLTypeName.STRING)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_int64() {
      val field = Field.of("age", StandardSQLTypeName.INT64)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_boolean() {
      val field = Field.of("active", StandardSQLTypeName.BOOL)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_float64() {
      val field = Field.of("price", StandardSQLTypeName.FLOAT64)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_date() {
      val field = Field.of("birthDate", StandardSQLTypeName.DATE)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_datetime() {
      val field = Field.of("createdAt", StandardSQLTypeName.DATETIME)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_timestamp() {
      val field = Field.of("updatedAt", StandardSQLTypeName.TIMESTAMP)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_time() {
      val field = Field.of("time", StandardSQLTypeName.TIME)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_json() {
      val field = Field.of("payload", StandardSQLTypeName.JSON)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_field_is_geography() {
      val field = Field.of("location", StandardSQLTypeName.GEOGRAPHY)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_array_mapper_when_field_mode_is_repeated() {
      val field = Field.newBuilder("tags", StandardSQLTypeName.STRING)
        .setMode(Field.Mode.REPEATED)
        .build()

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun should_use_metadata_subtype_when_field_is_repeated_with_metadata() {
      val field = Field.newBuilder("tags", StandardSQLTypeName.STRING)
        .setMode(Field.Mode.REPEATED)
        .build()
      val metadata = BigQueryMetadataFactory(subType = String::class.java)

      val mapper = factory.factory(field, metadata)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_struct_mapper_when_field_is_struct() {
      val structField = Field.newBuilder("address", StandardSQLTypeName.STRUCT,
        Field.of("street", StandardSQLTypeName.STRING),
        Field.of("city", StandardSQLTypeName.STRING)
      ).build()

      val mapper = factory.factory(structField)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_struct_mapper_when_field_is_nested_struct() {
      val nestedStruct = Field.newBuilder("location", StandardSQLTypeName.STRUCT,
        Field.newBuilder("coordinates", StandardSQLTypeName.STRUCT,
          Field.of("lat", StandardSQLTypeName.FLOAT64),
          Field.of("lng", StandardSQLTypeName.FLOAT64)
        ).build()
      ).build()

      val mapper = factory.factory(nestedStruct)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_array_of_struct_mapper_when_field_is_repeated_struct() {
      val arrayOfStructs = Field.newBuilder("items", StandardSQLTypeName.STRUCT,
        Field.of("id", StandardSQLTypeName.INT64),
        Field.of("name", StandardSQLTypeName.STRING)
      ).setMode(Field.Mode.REPEATED).build()

      val mapper = factory.factory(arrayOfStructs)

      assertNotNull(mapper)
    }

    @Test
    fun should_use_metadata_type_when_metadata_is_given() {
      val field = Field.of("customField", StandardSQLTypeName.STRING)
      val metadata = BigQueryMetadataFactory(String::class.java)

      val mapper = factory.factory(field, metadata)

      assertNotNull(mapper)
    }

    @Test
    fun should_use_column_metadata_when_struct_metadata_has_matching_column() {
      val structField = Field.newBuilder("address", StandardSQLTypeName.STRUCT,
        Field.of("zip", StandardSQLTypeName.INT64)
      ).build()
      val metadata = BigQueryMetadataFactory(
        structType = BigQueryStructType(columns = mapOf("zip" to BigQueryMetadataFactory(Long::class.javaObjectType)))
      )

      val mapper = factory.factory(structField, metadata)

      assertNotNull(mapper)
    }

    @Test
    fun should_use_all_metadata_when_struct_metadata_has_no_matching_column() {
      val structField = Field.newBuilder("address", StandardSQLTypeName.STRUCT,
        Field.of("zip", StandardSQLTypeName.INT64)
      ).build()
      val metadata = BigQueryMetadataFactory(
        structType = BigQueryStructType(all = BigQueryMetadataFactory(Long::class.javaObjectType))
      )

      val mapper = factory.factory(structField, metadata)

      assertNotNull(mapper)
    }

    @Test
    fun should_throw_error_when_no_mapper_found_for_type() {
      val field = Field.of("amount", StandardSQLTypeName.BIGNUMERIC)

      assertThrows<IllegalStateException> {
        factory.factory(field)
      }
    }
  }

  @Nested
  inner class FromType {

    @Test
    fun should_return_mapper_when_type_is_string() {
      val mapper = factory.fromType(String::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_type_is_long() {
      val mapper = factory.fromType(Long::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_type_is_int() {
      val mapper = factory.fromType(Int::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_type_is_boolean() {
      val mapper = factory.fromType(Boolean::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_type_is_double() {
      val mapper = factory.fromType(Double::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_type_is_localdate() {
      val mapper = factory.fromType(LocalDate::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_mapper_when_type_is_localdatetime() {
      val mapper = factory.fromType(LocalDateTime::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_list_mapper_when_type_is_collection_of_primitives() {
      val listType = parameterizedType(List::class.java, String::class.java)

      val mapper = factory.fromType(listType)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_list_mapper_when_type_is_collection_of_complex_type() {
      val listType = parameterizedType(ArrayList::class.java, NestedItem::class.java)

      val mapper = factory.fromType(listType)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_object_mapper_when_type_is_custom_class() {
      val mapper = factory.fromType(SimpleEntity::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_include_inherited_fields_when_type_has_superclass() {
      val mapper = factory.fromType(ChildEntity::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_json_mapper_when_type_is_parameterized_map() {
      val mapType = parameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)

      val mapper = factory.fromType(mapType)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_struct_mapper_when_type_is_non_map_parameterized_type() {
      val boxType = parameterizedType(Box::class.java, String::class.java)

      val mapper = factory.fromType(boxType)

      assertNotNull(mapper)
    }

    @Test
    fun should_return_cached_mapper_when_already_cached() {
      val cachedMapper = mock<BigQueryMapper<*>>()
      whenever(cache.get(eq("MAPPER::${Long::class.java.typeName}"), eq(BigQueryMapper::class.java)))
        .thenReturn(cachedMapper)

      val result = factory.fromType(Long::class.java)

      assertSame(cachedMapper, result)
    }

    @Test
    fun should_throw_error_when_type_is_unsupported() {
      val unsupportedType = Byte::class.java

      assertThrows<IllegalStateException> {
        factory.fromType(unsupportedType)
      }
    }
  }

  @Nested
  inner class FromSchema {

    @Test
    fun should_return_empty_map_when_schema_is_null() {
      val result = factory.fromSchema(null)

      assertTrue(result.isEmpty())
    }

    @Test
    fun should_return_mapper_for_each_field_when_schema_has_fields() {
      val schema = Schema.of(
        Field.of("id", StandardSQLTypeName.INT64),
        Field.of("name", StandardSQLTypeName.STRING),
        Field.of("active", StandardSQLTypeName.BOOL)
      )

      val mappers = factory.fromSchema(schema)

      assertEquals(3, mappers.size)
      assertTrue(mappers.containsKey("id"))
      assertTrue(mappers.containsKey("name"))
      assertTrue(mappers.containsKey("active"))
    }

    @Test
    fun should_use_metadata_for_matching_field_when_metadata_partially_provided() {
      val schema = Schema.of(
        Field.of("id", StandardSQLTypeName.INT64),
        Field.of("name", StandardSQLTypeName.STRING)
      )
      val metadata = mapOf(
        "name" to BigQueryMetadataFactory(String::class.java)
      )

      val mappers = factory.fromSchema(schema, metadata)

      assertEquals(2, mappers.size)
    }

    @Test
    fun should_return_mapper_for_struct_fields_when_schema_has_struct() {
      val schema = Schema.of(
        Field.of("id", StandardSQLTypeName.INT64),
        Field.newBuilder("address", StandardSQLTypeName.STRUCT,
          Field.of("street", StandardSQLTypeName.STRING),
          Field.of("city", StandardSQLTypeName.STRING)
        ).build()
      )

      val mappers = factory.fromSchema(schema)

      assertEquals(2, mappers.size)
      assertNotNull(mappers["address"])
    }
  }

  @Nested
  inner class ToMetadataFactoryFromType {

    @Test
    fun should_return_metadata_when_type_is_simple_class() {
      val metadata = factory.toMetadataFactory(String::class.java)

      assertEquals(String::class.java, metadata.type)
    }

    @Test
    fun should_return_metadata_when_type_is_parameterized_type_not_map() {
      val listType = parameterizedType(List::class.java, String::class.java)

      val metadata = factory.toMetadataFactory(listType)

      assertEquals(List::class.java, metadata.type)
      assertEquals(String::class.java, metadata.subType)
    }

    @Test
    fun should_return_map_metadata_with_recursive_value_metadata_when_type_is_parameterized_map() {
      val mapType = parameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)

      val metadata = factory.toMetadataFactory(mapType)

      assertEquals(Map::class.java, metadata.type)
      assertEquals(Int::class.javaObjectType, metadata.structType.all?.type)
    }

    @Test
    fun should_return_map_metadata_without_all_when_map_value_type_is_any() {
      val mapType = parameterizedType(Map::class.java, String::class.java, Any::class.java)

      val metadata = factory.toMetadataFactory(mapType)

      assertEquals(Map::class.java, metadata.type)
      assertNull(metadata.structType.all)
    }

    @Test
    fun should_return_map_metadata_with_default_any_value_when_type_is_raw_map_class() {
      val metadata = factory.toMetadataFactory(Map::class.java)

      assertEquals(Map::class.java, metadata.type)
      assertEquals(Any::class.java, metadata.structType.all?.type)
    }

    @Test
    fun should_call_cache_for_each_invocation_when_called_multiple_times_with_same_type() {
      factory.toMetadataFactory(String::class.java)
      factory.toMetadataFactory(String::class.java)

      verify(cache, times(2)).get(
        eq("METADATA_FACTORY::${String::class.java.typeName}"),
        eq(BigQueryMetadataFactory::class.java)
      )
    }

    @Test
    fun should_return_cached_metadata_when_already_cached() {
      val cachedMetadata = BigQueryMetadataFactory(String::class.java)
      whenever(cache.get(eq("METADATA_FACTORY::${String::class.java.typeName}"), eq(BigQueryMetadataFactory::class.java)))
        .thenReturn(cachedMetadata)

      val result = factory.toMetadataFactory(String::class.java)

      assertSame(cachedMetadata, result)
    }

    @Test
    fun should_throw_error_when_type_is_invalid() {
      val invalidType = object : Type {
        override fun getTypeName() = "InvalidType"
      }

      assertThrows<IllegalStateException> {
        factory.toMetadataFactory(invalidType)
      }
    }
  }

  @Nested
  inner class ToMetadataFactoryFromFieldAccessor {

    @Test
    fun should_return_struct_metadata_when_field_accessor_has_struct() {
      val accessor = mock<FieldAccessor<*>>()
      val innerAccessor = mock<FieldAccessor<*>>()
      whenever(accessor.struct).thenReturn(mapOf("field1" to innerAccessor))
      whenever(accessor.type).thenReturn(Map::class.java)
      whenever(innerAccessor.struct).thenReturn(emptyMap())
      whenever(innerAccessor.type).thenReturn(String::class.java)

      val metadata = factory.toMetadataFactory(accessor)

      assertEquals(Map::class.java, metadata.type)
    }

    @Test
    fun should_delegate_to_type_metadata_when_field_accessor_has_no_struct() {
      val accessor = mock<FieldAccessor<*>>()
      whenever(accessor.struct).thenReturn(emptyMap())
      whenever(accessor.type).thenReturn(String::class.java)

      val metadata = factory.toMetadataFactory(accessor)

      assertEquals(String::class.java, metadata.type)
    }
  }
}
