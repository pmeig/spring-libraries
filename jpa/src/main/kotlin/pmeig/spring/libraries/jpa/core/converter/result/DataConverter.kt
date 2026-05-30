package pmeig.spring.libraries.jpa.core.converter.result

import java.lang.reflect.Method
import java.lang.reflect.Type

private class DataMergeConverter(
  private val previous: DataConverter,
  private val current: DataConverter
): DataConverter {
  override fun typeChange(
    method: Method,
    returnType: Type
  ): Type? {
    val newType = previous.typeChange(method, returnType)!!
    return current.typeChange(method, newType)
  }

  override fun convert(result: Any?): Any? {
    return previous.convert(current.convert(result))
  }
}


fun interface DataConverter {

  fun typeChange(method: Method, returnType: Type): Type?

  fun convert(result: Any?): Any? = result

  fun andThen(dataConverter: DataConverter): DataConverter {
    return DataMergeConverter(this, dataConverter)
  }
}