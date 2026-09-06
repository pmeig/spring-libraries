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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cache.Cache
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.data.bigquery.MAPPERS_CACHE
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BigQueryMapperFactoryTest {

  private val jsonMapper = ObjectMapper()
  private val cacheManager = mock<DataCacheManager>()
  private val cache = mock<Cache>()
  private val factory: BigQueryMapperFactory = BigQueryMapperFactory(jsonMapper, cacheManager)

  @BeforeEach
  fun setUp() {
    whenever(cacheManager.getCache(MAPPERS_CACHE)).thenReturn(cache)
    whenever(cache.get(any<String>(), any<Class<*>>())).thenReturn(null)
  }

  @Nested
  inner class FactoryFromField {

    @Test
    fun `factory creates mapper for primitive field`() {
      val field = Field.of("name", StandardSQLTypeName.STRING)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates mapper for integer field`() {
      val field = Field.of("age", StandardSQLTypeName.INT64)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates mapper for boolean field`() {
      val field = Field.of("active", StandardSQLTypeName.BOOL)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates mapper for float field`() {
      val field = Field.of("price", StandardSQLTypeName.FLOAT64)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates mapper for date field`() {
      val field = Field.of("birthDate", StandardSQLTypeName.DATE)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates mapper for datetime field`() {
      val field = Field.of("createdAt", StandardSQLTypeName.DATETIME)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates mapper for timestamp field`() {
      val field = Field.of("updatedAt", StandardSQLTypeName.TIMESTAMP)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates mapper for time field`() {
      val field = Field.of("time", StandardSQLTypeName.TIME)

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates array mapper for repeated field`() {
      val field = Field.newBuilder("tags", StandardSQLTypeName.STRING)
        .setMode(Field.Mode.REPEATED)
        .build()

      val mapper = factory.factory(field)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates struct mapper for struct field`() {
      val structField = Field.newBuilder("address", StandardSQLTypeName.STRUCT,
        Field.of("street", StandardSQLTypeName.STRING),
        Field.of("city", StandardSQLTypeName.STRING)
      ).build()

      val mapper = factory.factory(structField)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates nested struct mapper`() {
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
    fun `factory uses metadata for field mapping`() {
      val field = Field.of("customField", StandardSQLTypeName.STRING)
      val metadata = BigQueryMetadataFactory(String::class.java)

      val mapper = factory.factory(field, metadata)

      assertNotNull(mapper)
    }

    @Test
    fun `factory creates array of structs mapper`() {
      val arrayOfStructs = Field.newBuilder("items", StandardSQLTypeName.STRUCT,
        Field.of("id", StandardSQLTypeName.INT64),
        Field.of("name", StandardSQLTypeName.STRING)
      ).setMode(Field.Mode.REPEATED).build()

      val mapper = factory.factory(arrayOfStructs)

      assertNotNull(mapper)
    }
  }

  @Nested
  inner class FromType {

    @Test
    fun `fromType creates mapper for String`() {
      val mapper = factory.fromType(String::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for Long`() {
      val mapper = factory.fromType(Long::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for Int`() {
      val mapper = factory.fromType(Int::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for Boolean`() {
      val mapper = factory.fromType(Boolean::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for Double`() {
      val mapper = factory.fromType(Double::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for LocalDate`() {
      val mapper = factory.fromType(LocalDate::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for LocalDateTime`() {
      val mapper = factory.fromType(LocalDateTime::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for Collection of primitives`() {
      val listType = object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}.type

      val mapper = factory.fromType(listType)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for Collection of complex types`() {

      val mapper = factory.fromType(object: ParameterizedType{
        override fun getActualTypeArguments(): Array<out Type> {
          return arrayOf(EntityTest::class.java)
        }

        override fun getRawType(): Type {
          return ArrayList::class.java
        }

        override fun getOwnerType(): Type {
          return rawType
        }
      } )

      assertNotNull(mapper)
    }

    @Test
    fun `fromType creates mapper for custom class`() {
      val mapper = factory.fromType(TestEntity::class.java)

      assertNotNull(mapper)
    }

    @Test
    fun `fromType throws error for unsupported type`() {
      val unsupportedType = Byte::class.java

      assertThrows<IllegalStateException> {
        factory.fromType(unsupportedType)
      }
    }
  }

  @Nested
  inner class FromSchema {

    @Test
    fun `fromSchema returns empty map for null schema`() {
      val result = factory.fromSchema(null)

      assertTrue(result.isEmpty())
    }

    @Test
    fun `fromSchema creates mappers for all schema fields`() {
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
    fun `fromSchema uses metadata for specific fields`() {
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
    fun `fromSchema handles complex schema with structs`() {
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
  inner class ToMetadataFactory {

    @Test
    fun `toMetadataFactory creates metadata for simple class`() {
      val metadata = factory.toMetadataFactory(String::class.java)

      assertNotNull(metadata)
      assertEquals(String::class.java, metadata.type)
    }

    @Test
    fun `toMetadataFactory creates metadata for parameterized type`() {
      val listType = object : com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}.type

      val metadata = factory.toMetadataFactory(listType)

      assertNotNull(metadata)
      assertIs<ParameterizedType>(listType)
      assertEquals(listType.rawType, metadata.type)
    }

    @Test
    fun `toMetadataFactory creates metadata for Map type`() {
      val mapType = object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Int>>() {}.type

      val metadata = factory.toMetadataFactory(mapType)

      assertNotNull(metadata)
      assertEquals(Map::class.java, metadata.type)
    }

    @Test
    fun `toMetadataFactory creates metadata for Map with Any value type`() {
      val mapType = object : com.fasterxml.jackson.core.type.TypeReference<Map<String, Any>>() {}.type

      val metadata = factory.toMetadataFactory(mapType)

      assertNotNull(metadata)
      assertNotNull(metadata.structType)
    }

    @Test
    fun `toMetadataFactory uses cache`() {
      factory.toMetadataFactory(String::class.java)
      factory.toMetadataFactory(String::class.java)

      verify(cache).get(
        eq("METADATA_FACTORY::${String::class.java.typeName}"),
        eq(BigQueryMetadataFactory::class.java)
      )
    }

    @Test
    fun `toMetadataFactory throws error for invalid type`() {
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
    fun `toMetadataFactory creates metadata from FieldAccessor with struct`() {
      val accessor = mock<FieldAccessor<*>>()
      val innerAccessor = mock<FieldAccessor<*>>()
      whenever(accessor.struct).thenReturn(mapOf("field1" to innerAccessor))
      whenever(accessor.type).thenReturn(Map::class.java)
      whenever(innerAccessor.struct).thenReturn(emptyMap())
      whenever(innerAccessor.type).thenReturn(String::class.java)

      val metadata = factory.toMetadataFactory(accessor)

      assertNotNull(metadata)
      assertEquals(Map::class.java, metadata.type)
    }

    @Test
    fun `toMetadataFactory creates metadata from FieldAccessor without struct`() {
      val accessor = mock<FieldAccessor<*>>()
      whenever(accessor.struct).thenReturn(emptyMap())
      whenever(accessor.type).thenReturn(String::class.java)

      val metadata = factory.toMetadataFactory(accessor)

      assertNotNull(metadata)
      assertEquals(String::class.java, metadata.type)
    }
  }

  // Test entity for testing purposes
  private data class TestEntity(
    val id: Long = 0,
    val name: String = "",
    val active: Boolean = false
  )
}
