package pmeig.spring.libraries.jpa.core.executor

import com.fasterxml.jackson.core.type.TypeReference
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
    fun should_name_parameters_positionally_when_no_Param_annotation() {
      val ctx = service.context(method("queryWithPositional", Long::class.java, String::class.java),
        String::class.java)

      assertEquals(2, ctx.parameters.size)
      assertEquals("${POSITIONAL_PARAMETER_NAME}0", ctx.parameters[0].name)
      assertEquals(0, ctx.parameters[0].position)
      assertEquals("${POSITIONAL_PARAMETER_NAME}1", ctx.parameters[1].name)
      assertEquals(1, ctx.parameters[1].position)
    }

    @Test
    fun should_use_Param_annotation_value_when_naming_parameter() {
      val ctx = service.context(method("queryWithNamed", String::class.java),
        String::class.java)

      assertEquals(1, ctx.parameters.size)
      assertEquals("name", ctx.parameters[0].name)
      assertEquals(0, ctx.parameters[0].position)
    }

    @Test
    fun should_not_flag_map_json_or_collection_when_return_type_is_a_plain_class() {
      val ctx = service.context(method("findBySecretNameAndColumn", String::class.java, String::class.java)
      , EntityTest::class.java)
      assertFalse(ctx.map)
      assertFalse(ctx.json)
      assertFalse(ctx.collection)
    }

    @Test
    fun should_flag_json_when_method_name_ends_with_toJson_and_returns_string() {
      val ctx = service.context(method("queryToJson", Long::class.java),
        String::class.java)

      assertTrue(ctx.json)
    }

    @Test
    fun should_flag_map_when_return_type_is_a_map() {
      val mapType = object : TypeReference<Map<String, String>>() {}.type

      val ctx = service.context(method("errorNoQuery"), mapType)

      assertTrue(ctx.map)
      assertFalse(ctx.collection)
    }

    @Test
    fun should_flag_map_and_collection_when_return_type_is_a_collection_of_maps() {
      val collectionOfMapType = object : TypeReference<List<Map<String, String>>>() {}.type

      val ctx = service.context(method("errorNoQuery"), collectionOfMapType)

      assertTrue(ctx.map)
      assertTrue(ctx.collection)
    }

    @Test
    fun should_not_flag_map_or_collection_when_return_type_is_not_parameterized() {
      val ctx = service.context(method("errorNoQuery"), String::class.java)

      assertFalse(ctx.map)
      assertFalse(ctx.collection)
    }

    @Test
    fun should_flag_batch_when_method_name_starts_with_batch() {
      val ctx = service.context(method("batchNoQuery"), String::class.java)

      assertTrue(ctx.batch)
    }

    @Test
    fun should_return_same_instance_when_invoked_twice_with_a_caching_cache() {
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
    fun should_return_null_when_method_has_no_Query_annotation() {
      val result = service.query(method("errorNoQuery"), String::class.java)

      assertNull(result)
    }

    @Test
    fun should_rewrite_positional_markers_to_named_placeholders_when_parameters_are_positional() {
      val result = service.query(method("queryWithPositional",
        Long::class.java, String::class.java), String::class.java)

      assertNotNull(result)
      assertEquals(
        "SELECT secret FROM entity_test WHERE id = :${POSITIONAL_PARAMETER_NAME}1 AND secret = :${POSITIONAL_PARAMETER_NAME}2",
        result.sql
      )
    }

    @Test
    fun should_keep_sql_unchanged_when_parameters_use_Param_annotation() {
      val result = service.query(method("queryWithNamed", String::class.java), String::class.java)

      assertNotNull(result)
      assertEquals("SELECT secret FROM entity_test WHERE secret = :name", result.sql)
    }
  }

  @Nested
  inner class Jpa {

    @Test
    fun should_return_invalid_context_when_method_is_not_findBy() {
      val context = service.jpa(method("errorNoQuery"))

      assertFalse(context.valid)
    }

    @Test
    fun should_return_valid_context_when_method_is_findBy() {
      val context = service.jpa(method("findBySecretName", String::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun should_return_first_element_when_postQuery_applied_for_findBy() {
      val context = service.jpa(method("findBySecretName", String::class.java))

      val items = listOf("a", "b")
      assertEquals("a", context.postQuery(items))
    }

    @Test
    fun should_return_valid_context_when_method_is_findAllBy() {
      val context = service.jpa(method("findAllByCreated", LocalDateTime::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun should_return_whole_collection_when_postQuery_applied_for_findAllBy() {
      val context = service.jpa(method("findAllByCreated", LocalDateTime::class.java))

      val items = listOf("a", "b")
      assertEquals(items, context.postQuery(items))
    }

    @Test
    fun should_return_valid_context_when_method_combines_predicates_with_And() {
      val context = service.jpa(method("findBySecretNameAndColumn", String::class.java, String::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun should_build_combined_specification_when_toSpecification_invoked_for_And_method() {
      val context = service.jpa(method("findBySecretNameAndColumn", String::class.java, String::class.java))

      val specification = context.toSpecification(arrayOf("secret", "col"))

      assertNotNull(specification)
    }

    @Test
    fun should_return_valid_context_when_method_combines_predicates_with_Or() {
      val context = service.jpa(method("findBySecretNameOrColumn", String::class.java, String::class.java))

      assertTrue(context.valid)
    }

    @Test
    fun should_return_valid_context_when_method_uses_Like_Not_In_IsNull_and_IsNotNull_suffixes() {
      assertTrue(service.jpa(method("findAllBySecretNameLike", String::class.java)).valid)
      assertTrue(service.jpa(method("findAllBySecretNameIn", Collection::class.java)).valid)
      assertTrue(service.jpa(method("findAllBySecretNameIsNull")).valid)
      assertTrue(service.jpa(method("findAllBySecretNameIsNotNull")).valid)
    }
  }
}
