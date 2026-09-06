package pmeig.spring.libraries.jpa.core.converter.result

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val dummyMethod = Any::class.java.getMethod("toString")

private fun parameterizedType(raw: Class<*>, arg: Type): ParameterizedType = object : ParameterizedType {
  override fun getRawType(): Type = raw
  override fun getOwnerType(): Type? = null
  override fun getActualTypeArguments(): Array<out Type> = arrayOf(arg)
}

private class IterableOnlySet(iterable: Iterable<String>) : AbstractSet<String>() {
  private val backing = iterable.toMutableList()
  override val size get() = backing.size
  override fun iterator() = backing.iterator()
}

class CollectionConverterTest {

  @Nested
  inner class SetConverterTest {

    private val converter = SetConverter()

    @Test
    fun should_return_null_when_return_type_is_not_parameterized_type() {
      assertNull(converter.typeChange(dummyMethod, String::class.java))
    }

    @Test
    fun should_return_null_when_return_type_is_not_a_set() {
      assertNull(converter.typeChange(dummyMethod, parameterizedType(List::class.java, String::class.java)))
    }

    @Test
    fun should_return_list_type_when_return_type_is_a_set() {
      val setType = parameterizedType(Set::class.java, String::class.java)

      val result = converter.typeChange(dummyMethod, setType) as ParameterizedType

      assertEquals(List::class.java, result.rawType)
      assertEquals(String::class.java, result.actualTypeArguments[0])
    }

    @Test
    fun should_return_null_when_converting_null_result() {
      assertNull(converter.convert(null))
    }

    @Test
    fun should_convert_list_to_set_when_converting_result() {
      val result = converter.convert(listOf("a", "b", "a"))

      assertEquals(setOf("a", "b"), result)
    }
  }

  @Nested
  inner class CollectionConverterTest {

    private val converter = CollectionConverter()

    @Test
    fun should_return_null_when_return_type_is_not_parameterized_type() {
      assertNull(converter.typeChange(dummyMethod, String::class.java))
    }

    @Test
    fun should_return_null_when_return_type_is_not_a_set() {
      assertNull(converter.typeChange(dummyMethod, parameterizedType(List::class.java, String::class.java)))
    }

    @Test
    fun should_return_null_when_converting_null_result() {
      assertNull(converter.convert(null))
    }

    @Test
    fun should_convert_using_collection_constructor_when_type_has_one() {
      val setType = parameterizedType(LinkedHashSet::class.java, String::class.java)

      converter.typeChange(dummyMethod, setType)
      val result = converter.convert(listOf("a", "b"))

      assertTrue(result is LinkedHashSet<*>)
      assertEquals(listOf("a", "b"), result.toList())
    }

    @Test
    fun should_fallback_to_iterable_constructor_when_no_collection_constructor_exists() {
      val setType = parameterizedType(IterableOnlySet::class.java, String::class.java)

      converter.typeChange(dummyMethod, setType)
      val result = converter.convert(listOf("a", "b"))

      assertTrue(result is IterableOnlySet)
      assertEquals(listOf("a", "b"), result.toList())
    }
  }
}
