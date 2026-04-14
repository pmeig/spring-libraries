package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.StandardSQLTypeName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
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
import java.time.LocalDateTime
import kotlin.test.expect

class BigQueryMapperFactoryTest {

  private val jsonMapper = mock<ObjectMapper>()
  private val bigQueryMapperFactory = BigQueryMapperFactory(jsonMapper)

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
        schema, mapOf("struct" to structMetadataFactory_expected()))
    }
  }

  @Test
  fun toMetadataFactory_structField() {
    val structAccessor = generateStructAccessor()
    expect(structMetadataFactory_expected()) { bigQueryMapperFactory.toMetadataFactory(structAccessor) }
  }

  @Test
  fun toMetadataFactory_arrayField() {
    val arrayAccessor = generateArrayAccessor()
    expect(arrayMetadataFactory_expected()) {
      bigQueryMapperFactory.toMetadataFactory(arrayAccessor)
    }
  }

  @Test
  fun factoryFromBigQueryField_REPEATED() {

    val array = array_field()
    val metadataFactory = arrayMetadataFactory_expected()

    expect(listOf("first", "second")) {
      bigQueryMapperFactory.factory(array, metadataFactory).map(array_result())
    }
  }

  @Test
  fun factoryFromBigQueryField_STRUCT() {
    val structField = struct_field()
    val metadataFactory = structMetadataFactory_expected()

    expect(mapOf("name" to "name", "age" to 1, "size" to 2L)) {
      bigQueryMapperFactory.factory(structField, metadataFactory)
        .map(struct_result())
    }

  }

}