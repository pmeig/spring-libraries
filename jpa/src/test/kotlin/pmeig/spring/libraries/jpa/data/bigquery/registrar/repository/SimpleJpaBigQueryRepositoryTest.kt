package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository

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
import org.springframework.data.jpa.domain.Specification
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimpleJpaBigQueryRepositoryTest {

  private val client = mock<BigQueryClient>()
  private val cacheManager = mock<DataCacheManager>()
  private val entityAnnotationReader = EntityAnnotationReader(cacheManager)
  val metadata = entityAnnotationReader.metadata(EntityTest::class.java)
  private val repository = SimpleJpaBigQueryRepository<EntityTest, Long>(
    client,
    cacheManager,
    entityAnnotationReader,
    EntityTest::class.javaObjectType
  )

  @Nested
  inner class BasicRepositoryOperations {

    @Test
    fun `flush does nothing`() {
      repository.flush()
      verify(client, never()).tryQuery(any(), any())
    }

    @Test
    fun `saveAndFlush delegates to save`() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      assertEquals(entity, repository.saveAndFlush(entity))
    }

    @Test
    fun `saveAllAndFlush delegates to saveAll`() {
      val entities = listOf(EntityTest(), EntityTest())
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertEquals(entities.size, repository.saveAllAndFlush(entities).size)
    }
  }

  @Nested
  inner class Querying {

    @Test
    fun `findAll returns list from client`() {
      whenever(client.tryEntities(eq(EntityTest::class.java), eq("SELECT * FROM ${metadata.table}"), any())).thenReturn(listOf(EntityTest()))

      val result = repository.findAll()

      assertEquals(1, result.size)
    }

    @Test
    fun `findAll by sort applies order`() {
      whenever(client
        .tryEntities(eq(EntityTest::class.java), eq("SELECT * FROM ${metadata.table} ORDER BY id"), any()))
        .thenReturn(listOf(EntityTest()))

      repository.findAll(Sort.by("id"))

      verify(client).tryEntities(eq(EntityTest::class.java), argThat { contains("ORDER BY") }, any())
    }

    @Test
    fun `findById returns empty optional when client returns null`() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(null)

      assertFalse(repository.findById(1L).isPresent)
    }

    @Test
    fun `existsById delegates to findById`() {
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertTrue(repository.existsById(1L))
    }

    @Test
    fun `count returns zero when client returns null`() {
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(null)

      assertEquals(0L, repository.count())
    }

    @Test
    fun `count with example delegates to client`() {
      val example = Example.of(EntityTest())
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(3L)

      assertEquals(3L, repository.count(example))
    }
  }

  @Nested
  inner class Deletion {

    @Test
    fun `deleteById delegates to client`() {
      repository.deleteById(1L)
      verify(client).tryQuery(argThat { contains("DELETE FROM") && contains("@id") }, any())
    }

    @Test
    fun `deleteAll deletes all rows`() {
      repository.deleteAll()
      verify(client).tryQuery(eq("DELETE FROM ${metadata.table} WHERE 1 = 1"), any())
    }

    @Test
    fun `deleteAllByIdInBatch uses ids parameter`() {
      repository.deleteAllByIdInBatch(listOf(1L, 2L))
      verify(client).tryBatch(argThat { contains("UNNEST(@ids)") }, any())
    }
  }

  @Nested
  inner class SaveOperations {

    @Test
    fun `save delegates to client and returns entity`() {
      val entity = EntityTest()
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(entity)

      assertEquals(entity, repository.save(entity))
    }

    @Test
    fun `saveAll with small amount saves sequentially`() {
      val entities = listOf(EntityTest(), EntityTest())
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      val result = repository.saveAll(entities)

      assertEquals(2, result.size)
    }

    @Test
    fun `saveAll with many entities uses temp table flow`() {
      val entities = listOf(EntityTest(), EntityTest(), EntityTest())
      whenever(client.tryQuery(any(), any())).thenReturn(mock())
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest(), EntityTest()))

      val result = repository.saveAll(entities)

      assertNotNull(result)
      assertEquals(3, result.size)
    }
  }

  @Nested
  inner class Specifications {

    @Test
    fun `findAll by specification delegates to client`() {
      val spec = Specification<EntityTest> { root, _, builder -> builder.like(root.get("column"), "'toto'") }
      whenever(client.tryEntities(eq(EntityTest::class), any(), any())).thenReturn(listOf(EntityTest()))

      val result = repository.findAll(spec)

      assertEquals(1, result.size)
      verify(client).tryEntities(eq(EntityTest::class), argThat { contains("column LIKE ? ", ignoreCase = true) }, any())
    }

    @Test
    fun `count by specification delegates to client`() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(5L)

      assertEquals(5L, repository.count(spec))
    }

    @Test
    fun `findAll pageable uses count and paging query`() {
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(4L)
      whenever(client.tryEntities(eq(EntityTest::class.java), any(), any())).thenReturn(listOf(EntityTest(), EntityTest(),
        EntityTest
      ()))

      val result = repository.findAll(PageRequest.of(0, 3))

      assertEquals(4L, result.totalElements)
      assertEquals(3, result.content.size)
    }

    @Test
    fun `findOne specification returns optional`() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.tryEntity(eq(EntityTest::class), any(), any())).thenReturn(EntityTest())

      assertTrue(repository.findOne(spec).isPresent)
    }

    @Test
    fun `exists specification delegates to count`() {
      val spec = Specification<EntityTest> { _, _, builder -> builder.conjunction() }
      whenever(client.trySingle(eq(Long::class), any(), any())).thenReturn(1L)

      assertTrue(repository.exists(spec))
    }
  }
}