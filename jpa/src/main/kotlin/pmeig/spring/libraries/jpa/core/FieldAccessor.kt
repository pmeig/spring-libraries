package pmeig.spring.libraries.jpa.core

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Type

interface FieldAccessor<T>: FieldGetter<T>, FieldSetter<T> {
  val struct: Map<String, FieldAccessor<*>>
}

interface FieldSetter<T> {
  val declared: Class<*>
  val java: Class<T>
  val type: Type
  fun set(entity: Any?, value: T?)
}

interface FieldGetter<T> {
  val declared: Class<*>
  val java: Class<T>
  val type: Type
  fun get(entity: Any?): T?
}

private fun getMethod(name: String, clazz: Class<*>): Method? = try {
  if (clazz.typeName == Any::class.java.typeName)
    null
  else
    clazz.getDeclaredMethod(name)
} catch (_: Throwable) {
  getMethod(name, clazz.superclass)
}?.apply { isAccessible = true }

@Suppress("UNCHECKED_CAST")
internal class MethodGetter<T>(field: Field): FieldGetter<T> {
  override val declared: Class<*> = field.declaringClass
  override val java: Class<T> = field.type as Class<T>
  override val type: Type = field.genericType
  private val getter: Method? = getMethod("get${field.name[0].uppercaseChar()}${field.name.substring(1)}", field.declaringClass)
  @Suppress("UNCHECKED_CAST")
  override fun get(entity: Any?): T? = getter?.invoke(entity) as T?
  fun isReadable() = getter != null
}

@Suppress("UNCHECKED_CAST")
internal class MethodSetter<T>(field: Field): FieldSetter<T> {
  override val declared: Class<*> = field.declaringClass
  override val java: Class<T> = field.type as Class<T>
  override val type: Type = field.genericType
  private val setter: Method? = getMethod("set${field.name[0].uppercaseChar()}${field.name.substring(1)}", field.declaringClass)
  override fun set(entity: Any?, value: T?) {
    setter?.invoke(entity, value)
  }
  fun isWritable() = setter != null
}

@Suppress("UNCHECKED_CAST")
open class FieldAccessorWrapper<T>(field: Field,
                                   override val struct: Map<String, FieldAccessor<*>> = emptyMap()): FieldAccessor<T> {
  override val declared: Class<*> = field.declaringClass
  override val java: Class<T> = field.type as Class<T>
  override val type: Type = field.genericType
  private var getter: (Any?) -> T?
  private var setter: (Any?, T?) -> Unit

  init {
    field.isAccessible = true
    getter = MethodGetter<T>(field).let { if (it.isReadable()) it::get else { entity: Any? -> field.get(entity) as T? } }
    setter = MethodSetter<T>(field).let { if(it.isWritable()) it::set else ({ entity: Any?, value: T? -> field.set(entity, value)}) }
  }
  override fun get(entity: Any?): T? = getter(entity)
  override fun set(entity: Any?, value: T?) = setter(entity, value)
  override fun toString(): String {
    return "FieldAccessorWrapper(struct=$struct, declared=$declared, java=$java, type=$type)"
  }


}

@Suppress("UNCHECKED_CAST")
class ParentFieldAccessor<T>(private val parent: Field, private val child: FieldAccessor<T>, struct: Map<String, FieldAccessor<*>> = emptyMap()):
  FieldAccessorWrapper<T>(parent, struct) {
  override fun get(entity: Any?): T? = getParent(entity)?.let { child.get(it) }
  override fun set(entity: Any?, value: T?){
    getParent(entity)?.let { child.set(it, value) }
  }

  private fun getParent(entity: Any?): Any? {
    return entity?.let {
      var parentValue = super.get(it) as Any?
      if (null == parentValue) {
        parentValue = parent.type.declaredConstructors.find { constructor -> constructor.parameterCount == 0 }?.newInstance()?.apply {
          super.set(entity, parentValue)
        }
      }
      parentValue
    }
  }
}