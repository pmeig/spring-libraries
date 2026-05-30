package pmeig.spring.libraries.jpa.core.executor

import java.lang.reflect.Method
import java.lang.reflect.Type

class JpaMethodInvokerResult(val result: Any?, val executed: Boolean = true) {
  constructor(executed: Boolean = false) : this(null, executed)
}

@FunctionalInterface
interface JpaMethodInvoker {
  fun invokeMethod(method: Method, returnType: Type, args: Array<Any?>): JpaMethodInvokerResult
}