package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.StandardSQLTypeName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.cache.support.NoOpCache
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.shared.expected.arrayMetadataFactory_expected
import pmeig.spring.libraries.jpa.shared.expected.generateArrayAccessor
import pmeig.spring.libraries.jpa.shared.expected.generateStructAccessor
import pmeig.spring.libraries.jpa.shared.expected.mapper_date_expected
import pmeig.spring.libraries.jpa.shared.expected.mapper_object_expected
import pmeig.spring.libraries.jpa.shared.expected.mapper_primitive_expected
import pmeig.spring.libraries.jpa.shared.expected.structMetadataFactory_expected
import pmeig.spring.libraries.jpa.shared.helper.array_field
import pmeig.spring.libraries.jpa.shared.helper.array_result
import pmeig.spring.libraries.jpa.shared.helper.entityTest_schema
import pmeig.spring.libraries.jpa.shared.helper.struct_field
import pmeig.spring.libraries.jpa.shared.helper.struct_result
import java.lang.reflect.ParameterizedType
import java.time.LocalDateTime
import kotlin.test.expect

class BigQueryMapperFactoryTest {

  private val jsonMapper = mock<ObjectMapper>()
  private val cacheManager = mock<DataCacheManager>()
  private val parameterizedMock = mock<ParameterizedType>()
  private val bigQueryMapperFactory = BigQueryMapperFactory(jsonMapper, cacheManager)

  @BeforeEach
  fun setUp() {
    whenever(cacheManager.getCache(any())).thenReturn(NoOpCache("noOpCache"))
  }

  @Test
  fun fromSchema() {
    val schema = entityTest_schema()

    expect(
      mapOf(
        "id" to mapper_primitive_expected(StandardSQLTypeName.INT64, Long::class),
        "created_by" to mapper_primitive_expected(StandardSQLTypeName.STRING, String::class),
        "created" to mapper_date_expected(StandardSQLTypeName.DATETIME, LocalDateTime::class),
        "updated_by" to mapper_primitive_expected(StandardSQLTypeName.STRING, String::class),
        "updated" to mapper_date_expected(StandardSQLTypeName.DATETIME, LocalDateTime::class),
        "struct" to mapper_object_expected(
          StandardSQLTypeName.STRUCT, Map::class, jsonMapper, null, mapOf(
            "name" to mapper_primitive_expected(StandardSQLTypeName.STRING, String::class),
            "age" to mapper_primitive_expected(StandardSQLTypeName.INT64, Int::class.javaPrimitiveType!!),
            "size" to mapper_primitive_expected(StandardSQLTypeName.INT64, Long::class)
          )
        ),
        "secret" to mapper_primitive_expected(StandardSQLTypeName.STRING, String::class),
        "array" to mapper_object_expected(
          StandardSQLTypeName.ARRAY, List::class, jsonMapper, mapper_primitive_expected(
            StandardSQLTypeName.STRING, String::class
          )
        )
      )
    ) {
      bigQueryMapperFactory.fromSchema(
        schema, mapOf("struct" to structMetadataFactory_expected())
      )
    }
  }

  @Nested
  inner class FromType {
    @Test
    fun `generate primitive mapper`() {
      expect(mapper_primitive_expected(StandardSQLTypeName.STRING, String::class)) {
        bigQueryMapperFactory.fromType(String::class.java)
      }
    }

    @Test
    fun `generate mapper for collection type`() {
      whenever(parameterizedMock.rawType).thenReturn(List::class.java)
      whenever(parameterizedMock.actualTypeArguments).thenReturn(arrayOf(String::class.java))
      whenever(parameterizedMock.typeName).thenReturn(List::class.java.typeName + "<${String::class.java.typeName}>")

      expect(
        mapper_object_expected(
          StandardSQLTypeName.ARRAY,
          List::class,
          jsonMapper,
          mapper_primitive_expected(StandardSQLTypeName.STRING, String::class)
        )
      ) {
        bigQueryMapperFactory.fromType(parameterizedMock)
      }
    }
  }

  @Nested
  inner class ToMetadataFactory {
    @Test
    fun `generate by struct`() {
      val structAccessor = generateStructAccessor()
      expect(structMetadataFactory_expected()) { bigQueryMapperFactory.toMetadataFactory(structAccessor) }
    }

    @Test
    fun `generate by array`() {
      val arrayAccessor = generateArrayAccessor()
      expect(arrayMetadataFactory_expected()) {
        bigQueryMapperFactory.toMetadataFactory(arrayAccessor)
      }
    }

    @Test
    fun `generate by map with value Any`() {
      whenever(parameterizedMock.rawType).thenReturn(Map::class.java)
      whenever(parameterizedMock.actualTypeArguments).thenReturn(arrayOf(String::class.java, Any::class.java))
      expect(BigQueryMetadataFactory(Map::class.java, null, BigQueryStructType())) {
        bigQueryMapperFactory.toMetadataFactory(parameterizedMock)
      }
    }

    @Test
    fun `generate by map with value String`() {
      whenever(parameterizedMock.rawType).thenReturn(Map::class.java)
      whenever(parameterizedMock.actualTypeArguments).thenReturn(arrayOf(String::class.java, String::class.java))
      expect(
        BigQueryMetadataFactory(
          Map::class.java, null, BigQueryStructType(
            emptyMap(), BigQueryMetadataFactory(
              String::class.java
            )
          )
        )
      ) {
        bigQueryMapperFactory.toMetadataFactory(parameterizedMock)
      }
    }
  }

  @Nested
  inner class Factory {
    @Test
    fun `from BigQueryField is repeated`() {

      val array = array_field()
      val metadataFactory = arrayMetadataFactory_expected()

      expect(listOf("first", "second")) {
        bigQueryMapperFactory.factory(array, metadataFactory).map(array_result())
      }
    }

    @Test
    fun `from BigQueryField is struct`() {
      val structField = struct_field()
      val metadataFactory = structMetadataFactory_expected()

      expect(mapOf("name" to "name", "age" to 1, "size" to 2L)) {
        bigQueryMapperFactory.factory(structField, metadataFactory)
          .map(struct_result())
      }

    }
  }
}