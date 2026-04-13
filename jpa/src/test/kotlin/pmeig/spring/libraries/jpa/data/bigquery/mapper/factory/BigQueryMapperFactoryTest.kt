package pmeig.spring.libraries.jpa.data.bigquery.mapper.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import pmeig.spring.libraries.jpa.shared.expected.generateArrayAccessor
import pmeig.spring.libraries.jpa.shared.expected.generateStructAccessor
import pmeig.spring.libraries.jpa.shared.expected.structMetadataFactory_expected
import kotlin.test.expect

class BigQueryMapperFactoryTest {

  private val jsonMapper = mock<ObjectMapper>()
  private val bigQueryMapperFactory = BigQueryMapperFactory(jsonMapper)
  private val bigQueryField = mock<Field>()

  @Test
  fun fromSchema() {
    
  }

  @Test
  fun toMetadataFactory_structField() {
    val structAccessor = generateStructAccessor()
    expect(structMetadataFactory_expected()) { bigQueryMapperFactory.toMetadataFactory(structAccessor)}
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
    whenever(bigQueryField.mode).thenReturn(Field.Mode.REPEATED)
    whenever(bigQueryField.type).thenReturn(LegacySQLTypeName.STRING)

    val array = generateArrayAccessor()
    val metadataFactory = toMetadataFactory_arrayField()
  }

}