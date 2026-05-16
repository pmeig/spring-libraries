package pmeig.spring.libraries.jpa.data.bigquery.registrar

import java.lang.reflect.Method

class JpaMethodInvokerResult internal constructor(val result: Any?, val executed: Boolean = true) {
  internal constructor(executed: Boolean = false) : this(null, executed)
}

interface JpaMethodInvoker {
  fun invokeMethod(method: Method, args: Array<Any?>): JpaMethodInvokerResult
}