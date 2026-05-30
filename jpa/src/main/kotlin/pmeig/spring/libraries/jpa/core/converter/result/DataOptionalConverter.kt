package pmeig.spring.libraries.jpa.core.converter.result

import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Optional

@Component
class DataOptionalConverter: DataConverter {

  override fun typeChange(
    method: Method,
    returnType: Type
  ): Type? {
    if (returnType !is ParameterizedType) return null
    if (returnType.rawType.typeName.startsWith(Optional::class.qualifiedName!!)) return returnType.actualTypeArguments[0]
    return null
  }

  override fun convert(result: Any?): Any? {
    return result as? Optional<*> ?: Optional.ofNullable(result)
  }
}