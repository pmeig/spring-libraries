package pmeig.spring.libraries.jpa.core.executor

import java.lang.reflect.Method
import java.lang.reflect.Type

class ObjectMethodInvoker(private val repository: Class<*>): JpaMethodInvoker {

  override fun invokeMethod(
    method: Method,
    returnType: Type,
    args: Array<Any?>
  ): JpaMethodInvokerResult {
    if (Any::class.java.isAssignableFrom(method.declaringClass)) {
      return JpaMethodInvokerResult(method.invoke(repository, *args), true)
    }
    return JpaMethodInvokerResult()
  }
}
