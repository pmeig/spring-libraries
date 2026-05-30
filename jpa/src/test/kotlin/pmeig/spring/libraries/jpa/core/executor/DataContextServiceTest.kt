package pmeig.spring.libraries.jpa.core.executor

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.NoOpCache
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.executor.DataContextService.Companion.POSITIONAL_PARAMETER_NAME
import pmeig.spring.libraries.jpa.shared.TestEntityRepository
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DataContextServiceTest {

  private val cacheManager = mock<DataCacheManager>()
  private val service = DataContextService(cacheManager)

  @BeforeEach
  fun setUp() {
    whenever(cacheManager.getCache(any())).thenReturn(NoOpCache("noOpCache"))
  }

  private fun method(name: String, vararg parameterTypes: Class<*>) =
    TestEntityRepository::class.java.getDeclaredMethod(name, *parameterTypes)

  @Nested
  inner class Context {

    @Test
    fun `returns method name and parameters with positional naming when no Param`() {
      val ctx = service.context(method("queryWithPositional", Long::class.java, String::class.java),
        String::class.java)

      assertEquals(2, ctx.parameters.size)
      assertEquals("${POSITIONAL_PARAMETER_NAME}0", ctx.parameters[0].name)
      assertEquals(0, ctx.parameters[0].position)
      assertEquals("${POSITIONAL_PARAMETER_NAME}1", ctx.parameters[1].name)
      assertEquals(1, ctx.parameters[1].position)
    }

    @Test
    fun `uses Param annotation value for parameter name`() {
      val ctx = service.context(method("queryWithNamed", String::class.java),
        String::class.java)

      assertEquals(1, ctx.parameters.size)
      assertEquals("name", ctx.parameters[0].name)
      assertEquals(0, ctx.parameters[0].position)
    }

    @Test
    fun `returns empty sql and empty postQuery flags for plain String returning method`() {
      val ctx = service.context(method("findBySecretNameAndColumn", String::class.java, String::class.java)
      , EntityTest::class.java)
      assertFalse(ctx.map)
      assertFalse(ctx.json)
      assertFalse(ctx.collection)
    }

    @Test
    fun `flags json when method name ends with toJson and returns String`() {
      val ctx = service.context(method("queryToJson", Long::class.java),
        String::class.java)

      assertTrue(ctx.json)
    }

    @Test
    fun `is cached on second invocation`() {
      val backingCache = ConcurrentMapCache("dataContext")
      whenever(cacheManager.getCache(any())).thenReturn(backingCache)

      val first = service.context(method("findBySecretNameAndColumn",
        String::class.java, String::class.java),
        EntityTest::class.java)
      val second = service.context(method("findBySecretNameAndColumn",
        String::class.java, String::class.java),
        EntityTest::class.java)

      assertSame(first, second)
    }
  }

  @Nested
  inner class Query {

    @Test
    fun `returns null when method has no Query annotation`() {
      val result = service.query(method("errorNoQuery"), String::class.java)

      assertNull(result)
    }

    @Test
    fun `rewrites positional markers to named placeholders when params are positional`() {
      val result = service.query(method("queryWithPositional",
        Long::class.java, String::class.java), String::class.java)

      assertNotNull(result)
      assertEquals(
        "SELECT secret FROM entity_test WHERE id = :${POSITIONAL_PARAMETER_NAME}1 AND secret = :${POSITIONAL_PARAMETER_NAME}2",
        result.sql
      )
    }

    @Test
    fun `keeps sql unchanged when parameters use Param annotation`() {
      val result = service.query(method("queryWithNamed", String::class.java), String::class.java)

      assertNotNull(result)
      assertEquals("SELECT secret FROM entity_test WHERE secret = :name", result.sql)
    }
  }

  @Nested
  inner class Jpa {

    @Test
    fun `returns invalid context for non findBy methods`() {
      val context = service.jpa(method("errorNoQuery"))

      assertFalse(context.valid)
    }

    @Test
    fun `returns valid context for findBy methods`() {
      val context = service.jpa(method("findBySecretName", String::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun `returns valid context for findAllBy methods`() {
      val context = service.jpa(method("findAllByCreated", LocalDateTime::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun `postQuery for findAllBy returns the whole collection`() {
      val context = service.jpa(method("findAllByCreated", LocalDateTime::class.java))

      val items = listOf("a", "b")
      assertEquals(items, context.postQuery(items))
    }

    @Test
    fun `parses And combinator without throwing`() {
      val context = service.jpa(method("findBySecretNameAndColumn", String::class.java, String::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun `parses Or combinator without throwing`() {
      val context = service.jpa(method("findBySecretNameOrColumn", String::class.java, String::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun `accepts predicate suffixes Like, Not, In, IsNull and IsNotNull`() {
      assertTrue(service.jpa(method("findAllBySecretNameLike", String::class.java)).valid)
      assertTrue(service.jpa(method("findAllBySecretNameIn", Collection::class.java)).valid)
      assertTrue(service.jpa(method("findAllBySecretNameIsNull")).valid)
      assertTrue(service.jpa(method("findAllBySecretNameIsNotNull")).valid)
    }
  }
}
