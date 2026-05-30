@file:Suppress("ReactiveStreamsUnusedPublisher")

package pmeig.spring.libraries.jpa.core.converter.result

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Component
@ConditionalOnClass(Flux::class)
class DataFluxConverter: DataConverter {
  override fun typeChange(
    method: Method,
    returnType: Type
  ): Type? {
    if (returnType !is ParameterizedType) return null
    if (returnType.rawType != Flux::class.java) return null
    return toList(returnType)
  }

  @Suppress("UNCHECKED_CAST")
  override fun convert(result: Any?): Any? {
    return Flux.fromIterable(result as? List<Any> ?: listOf())
  }
}

@Component
@ConditionalOnClass(Mono::class)
class DataMonoConverter: DataConverter {
  override fun typeChange(
    method: Method,
    returnType: Type
  ): Type? {
    if (returnType !is ParameterizedType) return null
    if (returnType.rawType != Mono::class.java) return null
    return returnType.actualTypeArguments[0]
  }

  override fun convert(result: Any?): Any? {
    return Mono.justOrEmpty(result)
  }
}