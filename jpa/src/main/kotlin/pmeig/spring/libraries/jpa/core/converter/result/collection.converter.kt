package pmeig.spring.libraries.jpa.core.converter.result

import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal fun toList(type: ParameterizedType) = object: ParameterizedType {
  override fun getRawType(): Type = List::class.java
  override fun getOwnerType() = rawType
  override fun getActualTypeArguments(): Array<out Type> = type.actualTypeArguments
}

@Component
class SetConverter: DataConverter {
  override fun typeChange(
    method: Method,
    returnType: Type
  ): Type? {
    if (returnType is ParameterizedType &&
      Set::class.java.isAssignableFrom(ClassUtils.forName(returnType.rawType.typeName, returnType.javaClass.classLoader))) {
      return toList(returnType)
    }
    return null
  }

  override fun convert(result: Any?): Any? {
    return (result as List<*>?)?.toSet()
  }
}

@Component
class CollectionConverter: DataConverter {

  private lateinit var constructor: Constructor<*>

  override fun typeChange(
    method: Method,
    returnType: Type
  ): Type? {
    if (returnType !is ParameterizedType) return null

    val clazz = ClassUtils.forName(returnType.rawType.typeName, returnType.javaClass.classLoader)
    if (!Collection::class.java.isAssignableFrom(clazz) || !Set::class.java.isAssignableFrom(clazz)
      || listOf(List::class.java, ArrayList::class.java).contains(clazz)
    ) return null

    try {
    constructor = clazz.getConstructor(Collection::class.java)
    } catch (_: NoSuchMethodException) {
      constructor = clazz.getConstructor(Iterable::class.java)
    }
    return toList(returnType)
  }

  override fun convert(result: Any?): Any? {
    return result?.let {
      constructor.newInstance(it)
    }
  }
}