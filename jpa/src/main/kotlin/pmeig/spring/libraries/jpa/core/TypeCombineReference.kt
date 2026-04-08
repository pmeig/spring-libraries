package pmeig.spring.libraries.jpa.core

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass

class TypeCombineReference(origin: ParameterizedType, private val target: KClass<*>): ParameterizedType {
  private val arguments = origin.actualTypeArguments
  override fun getActualTypeArguments(): Array<out Type> = arguments

  override fun getRawType(): Type = target.java

  override fun getOwnerType(): Type = rawType
}

class ParameterizedTypeReference<T>(
  private val type: Type,
  private vararg val arguments: Type
) : ParameterizedType {

  constructor(type: KClass<*>, vararg arguments: KClass<*>) : this(type, *arguments.map { it.java }.toTypedArray())
  constructor(type: KClass<*>, vararg arguments: Type) : this(type.java, *arguments)

  override fun getActualTypeArguments(): Array<out Type> = arguments
  override fun getRawType(): Type = type
  override fun getOwnerType(): Type = rawType
}