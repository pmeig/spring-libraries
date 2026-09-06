package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository

import com.google.cloud.bigquery.TableResult
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.NoResultException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.DeleteSpecification
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.UpdateSpecification
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import java.util.function.Function
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimpleJpaBigQueryRepositoryTest {

  private val client = mock<BigQueryClient>()
  private val cacheManager = mock<DataCacheManager>()
  private val entityAnnotationReader = EntityAnnotationReader(cacheManager)
  private val metadata = entityAnnotationReader.metadata(EntityTest::class.java)
  private val repository = SimpleJpaBigQueryRepository<EntityTest, Long>(
    client,
    cacheManager,
    entityAnnotationReader,
    EntityTest::class.javaObjectType
  )

  @Nested
  inner class InvokeMethod {

    @Test
    fun should_invoke_and_return_result_when_method_is_declared_on_repository() {
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(7L)
      val method = SimpleJpaBigQueryRepository::class.java.getMethod("count")

      val result = repository.invokeMethod(method, Long::class.java, arrayOf())

      assertTrue(result.executed)
      assertEquals(7L, result.result)
    }

    @Test
    fun should_return_non_executed_result_when_method_is_not_declared_on_repository() {
      val method = Any::class.java.getMethod("toString")

      val result = repository.invokeMethod(method, String::class.java, arrayOf())

      assertFalse(result.executed)
      assertEquals(null, result.result)
    }
  }

  @Nested
  inner class Flush {

    @Test
    fun should_do_nothing_when_called() {
      repository.flush()

      verify(client, never()).tryQuery(any(), any())
    }
  }

  @Nested
  inner class SaveAndFlush {

    @Test
    fun should_delegate_to_save_when_called() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      assertEquals(entity, repository.saveAndFlush(entity))
    }
  }

  @Nested
  inner class SaveAllAndFlush {

    @Test
    fun should_delegate_to_saveAll_when_called() {
      val entities = listOf(EntityTest(), EntityTest())
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertEquals(entities.size, repository.saveAllAndFlush(entities).size)
    }
  }

  @Nested
  inner class DeleteAllInBatch {

    @Test
    fun should_delete_all_rows_when_called_without_arguments() {
      repository.deleteAllInBatch()

      verify(client).tryBatch(eq("DELETE FROM ${metadata.table} WHERE 1 = 1"), any())
    }

    @Test
    fun should_delete_by_extracted_ids_when_called_with_entities() {
      val entities = listOf(EntityTest(id = 1L), EntityTest(id = 2L))

      repository.deleteAllInBatch(entities)

      verify(client).tryBatch(argThat { contains("UNNEST(@ids)") }, any())
    }
  }

  @Nested
  inner class DeleteAllByIdInBatch {

    @Test
    fun should_delete_by_ids_when_called() {
      repository.deleteAllByIdInBatch(listOf(1L, 2L))

      verify(client).tryBatch(argThat { contains("UNNEST(@ids)") }, any())
    }
  }

  @Nested
  inner class GetReferenceById {

    @Test
    fun should_return_entity_when_found() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      assertEquals(entity, repository.getReferenceById(1L))
    }

    @Test
    fun should_throw_entity_not_found_exception_when_not_found() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFailsWith<EntityNotFoundException> { repository.getReferenceById(1L) }
    }
  }

  @Suppress("DEPRECATION")
  @Nested
  inner class GetOne {

    @Test
    fun should_delegate_to_getReferenceById_when_called() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      assertEquals(entity, repository.getOne(1L))
    }
  }

  @Suppress("DEPRECATION")
  @Nested
  inner class GetById {

    @Test
    fun should_delegate_to_getReferenceById_when_called() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      assertEquals(entity, repository.getById(1L))
    }
  }

  @Nested
  inner class FindAll {

    @Test
    fun should_return_entities_when_called_without_arguments() {
      whenever(client.tryEntities(eq(EntityTest::class.java), eq("SELECT * FROM ${metadata.table}"), any()))
        .thenReturn(listOf(EntityTest()))

      val result = repository.findAll()

      assertEquals(1, result.size)
    }

    @Test
    fun should_apply_order_when_called_with_sort() {
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest()))

      repository.findAll(Sort.by("id"))

      verify(client).tryEntities(eq(EntityTest::class.java), argThat { contains("ORDER BY") }, any())
    }

    @Test
    fun should_return_entities_when_called_with_example() {
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest()))

      val result = repository.findAll(Example.of(EntityTest()))

      assertEquals(1, result.size)
      verify(client).tryEntities(eq(EntityTest::class.java), argThat { contains("FROM ${metadata.table}") }, any())
    }

    @Test
    fun should_apply_order_when_called_with_example_and_sort() {
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest()))

      repository.findAll(Example.of(EntityTest()), Sort.by("id"))

      verify(client).tryEntities(eq(EntityTest::class.java), argThat { contains("ORDER BY") }, any())
    }

    @Test
    fun should_return_page_when_called_with_pageable() {
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(4L)
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any()))
        .thenReturn(listOf(EntityTest(), EntityTest(), EntityTest()))

      val result = repository.findAll(PageRequest.of(0, 3))

      assertEquals(4L, result.totalElements)
      assertEquals(3, result.content.size)
    }

    @Test
    fun should_return_page_when_called_with_example_and_pageable() {
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(1L)
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest()))

      val result = repository.findAll(Example.of(EntityTest()), PageRequest.of(0, 1))

      assertEquals(1L, result.totalElements)
      assertEquals(1, result.content.size)
    }

    @Test
    fun should_return_entities_when_called_with_specification() {
      val spec = Specification<EntityTest> { root, _, builder -> builder.like(root.get("column"), "'toto'") }
      whenever(client.tryEntities(eq(EntityTest::class), any(), any())).thenReturn(listOf(EntityTest()))

      val result = repository.findAll(spec)

      assertEquals(1, result.size)
      verify(client).tryEntities(eq(EntityTest::class), argThat { contains("column LIKE ? ", ignoreCase = true) }, any())
    }

    @Test
    fun should_apply_order_when_called_with_specification_and_sort() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.tryEntities(eq(EntityTest::class), any(), any())).thenReturn(listOf(EntityTest()))

      repository.findAll(spec, Sort.by("id"))

      verify(client).tryEntities(eq(EntityTest::class), argThat { contains("ORDER BY") }, any())
    }

    @Test
    fun should_return_page_when_called_with_specification_and_pageable() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(2L)
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest(), EntityTest()))

      val result = repository.findAll(spec, PageRequest.of(0, 2))

      assertEquals(2L, result.totalElements)
      assertEquals(2, result.content.size)
    }

    @Test
    fun should_use_the_dedicated_count_specification_when_provided_separately() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      val countSpec = Specification<EntityTest> { _, _, builder -> builder.disjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(9L)
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest()))

      val result = repository.findAll(spec, countSpec, PageRequest.of(0, 1))

      assertEquals(9L, result.totalElements)
      assertEquals(1, result.content.size)
    }
  }

  @Nested
  inner class SaveAll {

    @Test
    fun should_save_sequentially_when_less_than_three_entities() {
      val entities = listOf(EntityTest(), EntityTest())
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      val result = repository.saveAll(entities)

      assertEquals(2, result.size)
      verify(client, never()).tryQuery(any(), any())
    }

    @Test
    fun should_use_temporary_table_flow_when_three_or_more_entities() {
      val entities = listOf(EntityTest(), EntityTest(), EntityTest())
      whenever(client.tryQuery(any(), any())).thenReturn(mock())
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest(), EntityTest()))

      val result = repository.saveAll(entities)

      assertEquals(3, result.size)
    }

    @Test
    fun should_throw_no_result_exception_when_temporary_table_flow_returns_no_entities() {
      val entities = listOf(EntityTest(), EntityTest(), EntityTest())
      whenever(client.tryQuery(any(), any())).thenReturn(mock())
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(emptyList())

      assertFailsWith<NoResultException> { repository.saveAll(entities) }
    }
  }

  @Nested
  inner class FindAllById {

    @Test
    fun should_return_entities_when_called_with_ids() {
      whenever(client.tryEntities(eq(EntityTest::class.java), argThat { contains("UNNEST(@ids)") }, any()))
        .thenReturn(listOf(EntityTest(), EntityTest()))

      val result = repository.findAllById(listOf(1L, 2L))

      assertEquals(2, result.size)
    }
  }

  @Nested
  inner class Save {

    @Test
    fun should_return_entity_when_client_returns_entity() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      assertEquals(entity, repository.save(entity))
    }

    @Test
    fun should_throw_entity_not_found_exception_when_client_returns_null() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFailsWith<EntityNotFoundException> { repository.save(EntityTest()) }
    }
  }

  @Nested
  inner class FindById {

    @Test
    fun should_return_present_optional_when_client_returns_entity() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      val result = repository.findById(1L)

      assertTrue(result.isPresent)
      assertEquals(entity, result.get())
    }

    @Test
    fun should_return_empty_optional_when_client_returns_null() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFalse(repository.findById(1L).isPresent)
    }
  }

  @Nested
  inner class ExistsById {

    @Test
    fun should_return_true_when_entity_found() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertTrue(repository.existsById(1L))
    }

    @Test
    fun should_return_false_when_entity_not_found() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFalse(repository.existsById(1L))
    }
  }

  @Nested
  inner class Count {

    @Test
    fun should_return_zero_when_client_returns_null() {
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(null)

      assertEquals(0L, repository.count())
    }

    @Test
    fun should_return_client_value_when_present() {
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(5L)

      assertEquals(5L, repository.count())
    }

    @Test
    fun should_return_zero_when_client_returns_null_for_example() {
      val example = Example.of(EntityTest())
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(null)

      assertEquals(0L, repository.count(example))
    }

    @Test
    fun should_return_client_value_when_present_for_example() {
      val example = Example.of(EntityTest())
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(3L)

      assertEquals(3L, repository.count(example))
    }

    @Test
    fun should_return_zero_when_client_returns_null_for_specification() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(null)

      assertEquals(0L, repository.count(spec))
    }

    @Test
    fun should_return_client_value_when_present_for_specification() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(5L)

      assertEquals(5L, repository.count(spec))
    }
  }

  @Nested
  inner class DeleteById {

    @Test
    fun should_delegate_to_client_when_called() {
      repository.deleteById(1L)

      verify(client).tryQuery(argThat { contains("DELETE FROM") && contains("@id") }, any())
    }
  }

  @Nested
  inner class Delete {

    @Test
    fun should_delegate_to_client_when_called_with_entity() {
      repository.delete(EntityTest(id = 1L))

      verify(client).tryQuery(argThat { contains("DELETE FROM") && contains("@id") }, any())
    }

    @Test
    fun should_return_total_rows_when_called_with_specification_and_client_returns_result() {
      val spec = DeleteSpecification<EntityTest> { root, _, builder -> builder.equal(root.get<Any>("column"), "toto") }
      val tableResult = mock<TableResult>()
      whenever(tableResult.totalRows).thenReturn(2L)
      whenever(client.tryQuery(any(), any())).thenReturn(tableResult)

      assertEquals(2L, repository.delete(spec))
      verify(client).tryQuery(argThat { contains("DELETE FROM") && contains("THEN RETURN *") }, any())
    }

    @Test
    fun should_return_zero_when_called_with_specification_and_client_returns_null() {
      val spec = DeleteSpecification<EntityTest> { root, _, builder -> builder.equal(root.get<Any>("column"), "toto") }
      whenever(client.tryQuery(any(), any())).thenReturn(null)

      assertEquals(0L, repository.delete(spec))
    }
  }

  @Nested
  inner class DeleteAllById {

    @Test
    fun should_delegate_to_client_when_called_with_ids() {
      repository.deleteAllById(listOf(1L, 2L))

      verify(client).tryQuery(argThat { contains("DELETE FROM") && contains("UNNEST(@id)") }, any())
    }

    @Test
    fun should_delegate_to_client_when_called_with_empty_ids() {
      repository.deleteAllById(emptyList())

      verify(client).tryQuery(argThat { contains("DELETE FROM") }, any())
    }
  }

  @Nested
  inner class DeleteAll {

    @Test
    fun should_delete_all_rows_when_called_without_arguments() {
      repository.deleteAll()

      verify(client).tryQuery(eq("DELETE FROM ${metadata.table} WHERE 1 = 1"), any())
    }

    @Test
    fun should_delegate_to_client_when_called_with_entities() {
      repository.deleteAll(listOf(EntityTest(id = 1L), EntityTest(id = 2L)))

      verify(client).tryQuery(argThat { contains("DELETE FROM") && contains("UNNEST(@id)") }, any())
    }

    @Test
    fun should_delegate_to_client_when_called_with_empty_entities() {
      repository.deleteAll(emptyList())

      verify(client).tryQuery(argThat { contains("DELETE FROM") }, any())
    }
  }

  @Nested
  inner class FindOne {

    @Test
    fun should_return_present_optional_when_called_with_example_and_client_returns_entity() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertTrue(repository.findOne(Example.of(EntityTest())).isPresent)
    }

    @Test
    fun should_return_empty_optional_when_called_with_example_and_client_returns_null() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFalse(repository.findOne(Example.of(EntityTest())).isPresent)
    }

    @Test
    fun should_return_present_optional_when_called_with_specification() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertTrue(repository.findOne(spec).isPresent)
    }

    @Test
    fun should_return_empty_optional_when_called_with_specification_and_client_returns_null() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFalse(repository.findOne(spec).isPresent)
    }
  }

  @Nested
  inner class Exists {

    @Test
    fun should_return_true_when_called_with_example_and_entity_found() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertTrue(repository.exists(Example.of(EntityTest())))
    }

    @Test
    fun should_return_false_when_called_with_example_and_entity_not_found() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFalse(repository.exists(Example.of(EntityTest())))
    }

    @Test
    fun should_return_true_when_called_with_specification_and_count_is_positive() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(1L)

      assertTrue(repository.exists(spec))
    }

    @Test
    fun should_return_false_when_called_with_specification_and_count_is_zero() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(0L)

      assertFalse(repository.exists(spec))
    }
  }

  @Nested
  inner class FindBy {

    @Test
    fun should_throw_unsupported_operation_exception_when_called_with_example() {
      assertFailsWith<UnsupportedOperationException> {
        repository.findBy(Example.of(EntityTest())) { it }
      }
    }

    @Test
    fun should_throw_unsupported_operation_exception_when_called_with_specification() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }

      assertFailsWith<UnsupportedOperationException> {
        repository.findBy<EntityTest, Any>(spec, Function { it })
      }
    }
  }

  @Nested
  inner class Update {

    @Test
    fun should_return_total_rows_when_client_returns_result() {
      val spec = UpdateSpecification<EntityTest> { _, update, builder ->
        update.set("column", "toto")
        builder.conjunction()
      }
      val tableResult = mock<TableResult>()
      whenever(tableResult.totalRows).thenReturn(3L)
      whenever(client.tryQuery(any(), any())).thenReturn(tableResult)

      assertEquals(3L, repository.update(spec))
      verify(client).tryQuery(argThat { contains("UPDATE") && contains("THEN RETURN *") }, any())
    }

    @Test
    fun should_return_zero_when_client_returns_null() {
      val spec = UpdateSpecification<EntityTest> { _, update, builder ->
        update.set("column", "toto")
        builder.conjunction()
      }
      whenever(client.tryQuery(any(), any())).thenReturn(null)

      assertEquals(0L, repository.update(spec))
    }
  }
}
