package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryParameterValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.shared.expected.entity_test_expected
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BigQueryFieldMapperTest {

  private val accessor = mock<FieldAccessor<Any>>()
  private val mapper = mock<BigQueryMapper<Any>>()

  private val fieldMapper = BigQueryFieldMapper(accessor, mapper)

  @Nested
  inner class Map {
    @Test
    fun `delegates mapping to mapper and accessor`() {
      val entity = entity_test_expected()
      val value = mock<FieldValue>()
      val mappedValue = "mapped"
      val updatedEntity = entity_test_expected().apply {
        column = mappedValue
      }

      whenever(mapper.map(value)).thenReturn(mappedValue)
      whenever(accessor.set(entity, mappedValue)).thenAnswer {
        (it.arguments.first() as EntityTest).column = it.arguments.last() as String?
      }

      fieldMapper.map(entity, value)

      assertEquals(updatedEntity, entity)
      verify(mapper).map(value)
      verify(accessor).set(entity, mappedValue)
    }
  }

  @Nested
  inner class Parameter {
    @Test
    fun `delegates parameter creation to accessor and mapper`() {
      val entity = entity_test_expected()
      val parameterValue = mock<QueryParameterValue>()

      whenever(accessor.get(entity)).thenReturn("value")
      whenever(mapper.parameter("value")).thenReturn(parameterValue)

      val actual = fieldMapper.parameter(entity)

      assertEquals(parameterValue, actual)
      verify(accessor).get(entity)
      verify(mapper).parameter("value")
    }

    @Test
    fun `throws when mapper returns null`() {
      val entity = entity_test_expected()

      whenever(accessor.get(entity)).thenReturn(null)
      whenever(mapper.parameter(null)).thenReturn(null)

      assertFailsWith<NullPointerException> {
        fieldMapper.parameter(entity)
      }
    }
  }
}