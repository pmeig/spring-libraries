package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository

import pmeig.spring.libraries.jpa.core.executor.DataContextService
import pmeig.spring.libraries.jpa.core.executor.JpaMethodInvoker
import pmeig.spring.libraries.jpa.core.executor.JpaMethodInvokerResult
import java.lang.reflect.Method
import java.lang.reflect.Type

class MethodNameBigQueryJpaRepository(
  private val simpleJpaBigQueryRepository: SimpleJpaBigQueryRepository,
  private val dataContextService: DataContextService,
): JpaMethodInvoker {
  override fun invokeMethod(
    method: Method,
    returnType: Type,
    args: Array<Any?>
  ): JpaMethodInvokerResult = dataContextService.jpa(method).let {
    if (it.valid) JpaMethodInvokerResult(
      it.postQuery(simpleJpaBigQueryRepository.findAll(it.toSpecification(args)))
    ) else JpaMethodInvokerResult()
  }
}