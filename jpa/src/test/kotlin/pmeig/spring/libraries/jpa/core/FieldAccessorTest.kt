package pmeig.spring.libraries.jpa.core

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import pmeig.spring.libraries.jpa.core.converter.configuration.formatter.DateTimeFormatterProvider
import pmeig.spring.libraries.jpa.core.converter.configuration.zoneId.DataZoneIdProvider
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private open class BaseWithValueGetter {
  fun getValue(): String = "fromBase"
}

private class InheritedGetterHolder(@JvmField var value: String = "own") : BaseWithValueGetter()

private class NoAccessorHolder(@JvmField var value: String = "own")

private class NormalizingHolder {
  var value: String? = null
    get() = field?.lowercase()
    set(v) { field = v?.uppercase() }
}

private class TrackingHolder {
  var getterCalls = 0
    private set
  var setterCalls = 0
    private set
  var value: String? = "initial"
    get() { getterCalls++; return field }
    set(v) { setterCalls++; field = v }
}

private class StructTarget {
  var value: String? = null
}

private class ChildHolder {
  var name: String? = null
}

private class ParentHolder {
  var child: ChildHolder? = null
}

private fun field(clazz: Class<*>, name: String) = clazz.getDeclaredField(name)

class FieldAccessorTest {

  @Nested
  inner class GetMethodResolution {

    @Test
    fun should_find_getter_declared_on_same_class_when_property_is_a_kotlin_var() {
      val getter = MethodGetter<String?>(field(NormalizingHolder::class.java, "value"))

      assertTrue(getter.isReadable())
    }

    @Test
    fun should_find_getter_on_superclass_when_declaring_class_has_no_matching_method() {
      val getter = MethodGetter<String>(field(InheritedGetterHolder::class.java, "value"))

      assertTrue(getter.isReadable())
      assertEquals("fromBase", getter.get(InheritedGetterHolder()))
    }

    @Test
    fun should_report_not_readable_when_no_getter_exists_up_to_Any() {
      val getter = MethodGetter<String>(field(NoAccessorHolder::class.java, "value"))

      assertFalse(getter.isReadable())
      assertNull(getter.get(NoAccessorHolder()))
    }
  }

  @Nested
  inner class MethodSetterTest {

    @Test
    fun should_report_not_writable_when_only_a_single_argument_setter_exists() {
      val holder = NormalizingHolder()
      val setter = MethodSetter<String?>(field(NormalizingHolder::class.java, "value"))

      assertFalse(setter.isWritable())
      setter.set(holder, "abc")

      val backingField = field(NormalizingHolder::class.java, "value").apply { isAccessible = true }
      assertNull(backingField.get(holder))
    }

    @Test
    fun should_do_nothing_when_no_setter_exists() {
      val holder = NoAccessorHolder()
      val setter = MethodSetter<String>(field(NoAccessorHolder::class.java, "value"))

      assertFalse(setter.isWritable())
      setter.set(holder, "ignored")

      assertEquals("own", holder.value)
    }
  }

  @Nested
  inner class FieldAccessorWrapperTest {

    @Test
    fun should_read_and_write_raw_field_when_no_accessor_methods_exist() {
      val wrapper = FieldAccessorWrapper<String>(field(NoAccessorHolder::class.java, "value"))
      val holder = NoAccessorHolder()

      assertEquals("own", wrapper.get(holder))

      wrapper.set(holder, "new")

      assertEquals("new", holder.value)
    }

    @Test
    fun should_use_declared_getter_method_when_available() {
      val wrapper = FieldAccessorWrapper<String?>(field(TrackingHolder::class.java, "value"))
      val holder = TrackingHolder()

      val initial = wrapper.get(holder)

      assertEquals("initial", initial)
      assertTrue(holder.getterCalls > 0)
    }

    @Test
    fun should_bypass_single_argument_setter_method_and_write_the_raw_field() {
      val wrapper = FieldAccessorWrapper<String?>(field(TrackingHolder::class.java, "value"))
      val holder = TrackingHolder()

      wrapper.set(holder, "updated")

      assertEquals(0, holder.setterCalls)
      val backingField = field(TrackingHolder::class.java, "value").apply { isAccessible = true }
      assertEquals("updated", backingField.get(holder))
    }

    @Test
    fun should_delegate_to_struct_accessors_when_value_is_a_map() {
      val cityAccessor = mock<FieldAccessor<Any?>>()
      val struct = mapOf<String, FieldAccessor<*>>("city" to cityAccessor)
      val wrapper = FieldAccessorWrapper<Any?>(field(StructTarget::class.java, "value"), struct)
      val holder = StructTarget()

      wrapper.set(holder, mapOf("city" to "Paris", "unknown" to "ignored"))

      verify(cityAccessor).set(holder, "Paris")
      assertNull(holder.value)
    }

    @Test
    fun should_fallback_to_original_setter_when_struct_is_defined_but_value_is_not_a_map() {
      val cityAccessor = mock<FieldAccessor<Any?>>()
      val struct = mapOf<String, FieldAccessor<*>>("city" to cityAccessor)
      val wrapper = FieldAccessorWrapper<Any?>(field(StructTarget::class.java, "value"), struct)
      val holder = StructTarget()

      wrapper.set(holder, "direct")

      assertEquals("direct", holder.value)
      verify(cityAccessor, never()).set(any(), any())
    }

    @Test
    fun should_be_equal_and_have_same_hashcode_when_same_field_and_struct() {
      val wrapperA = FieldAccessorWrapper<String>(field(NoAccessorHolder::class.java, "value"))
      val wrapperB = FieldAccessorWrapper<String>(field(NoAccessorHolder::class.java, "value"))

      assertEquals(wrapperA, wrapperA)
      assertEquals(wrapperA, wrapperB)
      assertEquals(wrapperA.hashCode(), wrapperB.hashCode())
    }

    @Test
    fun should_not_be_equal_when_other_type_differs() {
      val wrapper = FieldAccessorWrapper<String>(field(NoAccessorHolder::class.java, "value"))

      assertFalse(wrapper.equals("not-a-wrapper"))
    }

    @Test
    fun should_not_be_equal_when_field_metadata_differs() {
      val wrapperA = FieldAccessorWrapper<String>(field(NoAccessorHolder::class.java, "value"))
      val wrapperB = FieldAccessorWrapper<String?>(field(NormalizingHolder::class.java, "value"))

      assertFalse(wrapperA.equals(wrapperB))
    }

    @Test
    fun should_include_struct_declared_java_and_type_in_toString() {
      val wrapper = FieldAccessorWrapper<String>(field(NoAccessorHolder::class.java, "value"))

      val text = wrapper.toString()

      assertTrue(text.startsWith("FieldAccessorWrapper("))
      assertTrue(text.contains("declared="))
    }
  }

  @Nested
  inner class ParentFieldAccessorTest {

    private val childField = field(ChildHolder::class.java, "name")
    private val parentField = field(ParentHolder::class.java, "child")

    private fun childAccessor() = FieldAccessorWrapper<String?>(childField)

    @Test
    fun should_return_null_when_entity_is_null() {
      val accessor = ParentFieldAccessor(parentField, childAccessor())

      assertNull(accessor.get(null))
    }

    @Test
    fun should_create_missing_parent_instance_when_getting_value() {
      val accessor = ParentFieldAccessor(parentField, childAccessor())
      val holder = ParentHolder()

      val result = accessor.get(holder)

      assertNull(result)
      assertNotNull(holder.child)
      assertNull(holder.child?.name)
    }

    @Test
    fun should_reuse_existing_parent_instance_when_getting_value() {
      val accessor = ParentFieldAccessor(parentField, childAccessor())
      val existingChild = ChildHolder().apply { name = "preset" }
      val holder = ParentHolder().apply { child = existingChild }

      val result = accessor.get(holder)

      assertEquals("preset", result)
      assertSame(existingChild, holder.child)
    }

    @Test
    fun should_create_missing_parent_instance_when_setting_value() {
      val accessor = ParentFieldAccessor(parentField, childAccessor())
      val holder = ParentHolder()

      accessor.set(holder, "new-name")

      assertEquals("new-name", holder.child?.name)
    }

    @Test
    fun should_reuse_existing_parent_instance_when_setting_value() {
      val accessor = ParentFieldAccessor(parentField, childAccessor())
      val existingChild = ChildHolder()
      val holder = ParentHolder().apply { child = existingChild }

      accessor.set(holder, "renamed")

      assertSame(existingChild, holder.child)
      assertEquals("renamed", existingChild.name)
    }

    @Test
    fun should_expose_child_metadata_instead_of_parent_metadata() {
      val accessor = ParentFieldAccessor(parentField, childAccessor())

      assertEquals(ChildHolder::class.java, accessor.declared)
      assertEquals<Class<*>>(String::class.java, accessor.java)
    }

    @Test
    fun should_be_equal_when_same_parent_and_child() {
      val accessorA = ParentFieldAccessor(parentField, childAccessor())
      val accessorB = ParentFieldAccessor(parentField, childAccessor())

      assertEquals(accessorA, accessorA)
      assertEquals(accessorA, accessorB)
      assertEquals(accessorA.hashCode(), accessorB.hashCode())
    }

    @Test
    fun should_not_be_equal_when_child_differs() {
      val accessorA = ParentFieldAccessor(parentField, childAccessor())
      val accessorB = ParentFieldAccessor(parentField, FieldAccessorWrapper<String?>(childField, mapOf("x" to mock<FieldAccessor<*>>())))

      assertFalse(accessorA == accessorB)
    }

    @Test
    fun should_not_be_equal_when_other_type_differs() {
      val accessor = ParentFieldAccessor(parentField, childAccessor())

      assertFalse(accessor == childAccessor())
    }
  }

  @Nested
  inner class ZoneIdProviderAccessor {

    @AfterEach
    fun resetInstance() {
      zoneIdProviderInstance = null
    }

    @Test
    fun should_return_registered_instance_when_zoneIdProviderInstance_is_set() {
      val provider = mock<DataZoneIdProvider>()
      zoneIdProviderInstance = provider

      assertSame(provider, ZONE_ID_PROVIDER)
    }

    @Test
    fun should_throw_when_zoneIdProviderInstance_is_not_set() {
      zoneIdProviderInstance = null

      assertFailsWith<NullPointerException> { ZONE_ID_PROVIDER }
    }
  }

  @Nested
  inner class DateTimeFormatterProviderAccessor {

    @AfterEach
    fun resetInstance() {
      dateTimeFormatterInstance = null
    }

    @Test
    fun should_return_registered_instance_when_dateTimeFormatterInstance_is_set() {
      val provider = mock<DateTimeFormatterProvider>()
      dateTimeFormatterInstance = provider

      assertSame(provider, DATE_TIME_FORMATTER_PROVIDER)
    }

    @Test
    fun should_throw_when_dateTimeFormatterInstance_is_not_set() {
      dateTimeFormatterInstance = null

      assertFailsWith<NullPointerException> { DATE_TIME_FORMATTER_PROVIDER }
    }
  }
}
