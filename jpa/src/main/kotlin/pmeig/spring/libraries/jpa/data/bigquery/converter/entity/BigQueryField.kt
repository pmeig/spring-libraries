@file:Suppress("UNCHECKED_CAST")

package pmeig.spring.libraries.jpa.data.bigquery.converter.entity

import java.lang.reflect.Field
import java.lang.reflect.Method

interface BigQueryField<T> {
  val name: String
  val declaringClass: Class<*>
  val type: Class<T>
  var isAccessible: Boolean
  fun get(obj: Any?): T?
  fun set(obj: Any?, value: T?)
}

internal class SubField<T>(private val field: FieldAccessor<Any>, private val delegate: BigQueryField<T>) : BigQueryField<T> {
  init {
    delegate.isAccessible = true
  }

  override val declaringClass: Class<*> = delegate.declaringClass
  override val name: String = delegate.name
  override val type: Class<T> = delegate.type
  override var isAccessible: Boolean = true

  override fun get(obj: Any?): T? = obj
    ?.let { getParent(it) }
    ?.let { delegate.get(it) as T? }

  override fun set(obj: Any?, value: T?) {
    obj?.let { getParent(it) }?.let {
      delegate.set(it, value)
    }
  }

  private fun getParent(obj: Any): Any? {
    var parent = field.get(obj)
    if (null == parent) {
      parent =  delegate.declaringClass.declaredConstructors.find { it.parameterCount == 0 }?.newInstance()
      field.set(obj, parent)
    }
    return parent
  }
}

internal class FieldWrapper<T>(field: Field, type: Class<*>) : BigQueryField<T>, FieldAccessor<T> {
  private val getter: FieldGetter<T>
  private val setter: FieldSetter<T>
  init {
    val suffixMethod = field.name[0].uppercaseChar() + field.name.substring(1)
    val (getterMethod, setterMethod) = findMethods(suffixMethod, type)
    val accessor = FieldAccessorImpl<T>(field)
    getter = getterMethod?.let { MethodGetter(it) } ?: accessor
    setter = setterMethod?.let { MethodSetter(it) } ?: accessor
  }
  override var isAccessible: Boolean = true
  override val declaringClass: Class<*> = field.declaringClass
  override val type: Class<T> = field.type as Class<T>
  override val name: String = field.name
  override fun get(obj: Any?): T? = obj?.let {
    getter.get(it)
  }

  override fun set(obj: Any?, value: T?) {
    obj?.let {
      setter.set(it, value)
    }
  }

  private fun findOnceAccessor(name: String, type: Class<*>): Method? {
    if (type.typeName == Any::class.java.typeName) return null
    return getAccessorMethod(type, name) ?: findOnceAccessor(name, type.superclass)
  }

  private fun findMethods(suffixMethod: String, type: Class<*>): Pair<Method?, Method?> {
    if (type.typeName == Any::class.java.typeName) return Pair(null, null)
    val getter = getAccessorMethod(type, "get$suffixMethod")
    val setter = getAccessorMethod(type, "set$suffixMethod")
    if (getter != null && setter != null) return Pair(getter, setter)
    if (getter != null) return Pair(getter, findOnceAccessor("set$suffixMethod", type))
    if (setter != null) return Pair(findOnceAccessor("get$suffixMethod", type), setter)
    return Pair(null, null)
  }

  private fun getAccessorMethod(type: Class<*>, name: String) = try {
    type.getDeclaredMethod(name)
  } catch (e: NoSuchMethodException) {
    null
  }

}


internal interface FieldGetter<T> {
  fun get(obj: Any?): T?
}

internal interface FieldSetter<T> {
  fun set(obj: Any?, value: T?)
}

internal interface FieldAccessor<T> : FieldGetter<T>, FieldSetter<T>

private class MethodGetter<T>(private val getter: Method): FieldGetter<T> {
  init {
    getter.isAccessible = true
  }
  override fun get(obj: Any?): T? = obj?.let { getter.invoke(it) as T? }
}

private class MethodSetter<T>(private val setter: Method): FieldSetter<T> {
  init {
    setter.isAccessible = true
  }
  override fun set(obj: Any?, value: T?) {
    setter.invoke(obj, value)
  }
}

private class FieldAccessorImpl<T>(private val field: Field): FieldAccessor<T> {
  init {
    field.isAccessible = true
  }
  override fun get(obj: Any?): T? = obj?.let { field.get(it) as T? }
  override fun set(obj: Any?, value: T?) {
    obj?.let { field.set(it, value) }
  }
}
