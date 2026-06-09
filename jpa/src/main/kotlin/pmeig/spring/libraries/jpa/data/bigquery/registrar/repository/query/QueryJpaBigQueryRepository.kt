package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository.query

import com.google.cloud.bigquery.QueryJobConfiguration
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.executor.DataContextService
import pmeig.spring.libraries.jpa.core.executor.model.ParameterContext
import pmeig.spring.libraries.jpa.data.bigquery.JPA_REGISTRAR
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.core.executor.JpaMethodInvoker
import pmeig.spring.libraries.jpa.core.executor.JpaMethodInvokerResult
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.UnaryOperator

class QueryJpaBigQueryRepository(
  private val client: BigQueryClient,
  private val cacheManager: DataCacheManager,
  private val dataContextService: DataContextService,
  private val bigQueryMapperFactory: BigQueryMapperFactory
): JpaMethodInvoker {

  @Suppress("UNCHECKED_CAST")
  override fun invokeMethod(
    method: Method,
    returnType: Type,
    args: Array<Any?>
  ): JpaMethodInvokerResult = DataCacheNames.useCache(cacheManager, JPA_REGISTRAR, DataCacheNames.generateKeyFromMethod(method, "query"),
    BigQueryAnnotationQueryExecutor::class) {
    val queryContext = dataContextService.query(method, returnType) ?: return@useCache BigQueryAnnotationQueryExecutor()
    val parameters = queryContext.context.parameters.toMutableList()
    val configuratorIndex = extractIndexArgumentConfigurator(parameters)
    val configurator = createConfigurator(parameters, configuratorIndex, args)
    BigQueryAnnotationQueryExecutor(queryContext.sql, configurator, returnType, queryContext.context)
  }.let {
    if (it.query.isEmpty()) JpaMethodInvokerResult() else JpaMethodInvokerResult(it.execute(client, args), true)
  }

  private fun createConfigurator(
    parameters: List<ParameterContext>,
    configuratorIndex: Int,
    args: Array<Any?>
  ): (Array<Any?>) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder {
    var configurator = parameters.map {
      createConfiguratorByArguments(it)
    }.reduce { acc, configurer ->
      { methodParameters ->
        {
          configurer(methodParameters)(acc(methodParameters)(it))
        }
      }
    }
    if (configuratorIndex != -1) {
      configurator = extractCustomConfigurator(configurator, configuratorIndex, args[configuratorIndex])
    }
    return configurator
  }

  @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
  private fun extractCustomConfigurator(
    previous: (Array<Any?>) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder,
    configuratorIndex: Int,
    consumer: Any?
  ): (Array<Any?>) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder {
    val custom: (Any) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = when (consumer) {
      is Consumer<*> -> {
        {
          { builder ->
            (it as Consumer<QueryJobConfiguration.Builder>).accept(builder)
            builder
          }
        }
      }

      is Function<*, *>, is UnaryOperator<*> -> {
        {
          { builder ->
            (it as Function<QueryJobConfiguration.Builder, QueryJobConfiguration.Builder>).apply(builder)
          }
        }
      }

      else -> {
        if (consumer as? Function1<QueryJobConfiguration.Builder, QueryJobConfiguration.Builder> != null) {
          {
            { builder ->
              (it as Function1<QueryJobConfiguration.Builder, QueryJobConfiguration.Builder>)(builder)
            }
          }
        } else ({
          { builder ->
            (it as Function1<QueryJobConfiguration.Builder, Unit>)(builder)
            builder
          }
        })
      }

    }
    return {
      { builder ->
        custom(it[configuratorIndex]!!)(previous(it)(builder))
      }
    }
  }

  private fun createConfiguratorByArguments(context: ParameterContext): (Array<Any?>) -> (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder {
    val mapper = bigQueryMapperFactory.fromType(context.type)
    val index = context.position
    val name = context.name
    return { parameterValues ->
      { builder ->
        mapper.parameter(parameterValues[index])?.let {
          builder.addNamedParameter(name, it)
        } ?: builder
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractIndexArgumentConfigurator(parameters: MutableList<ParameterContext>): Int {
    val configuratorIndex = parameters.indexOfFirst {
      it.type is ParameterizedType && it.type.actualTypeArguments[0].typeName.startsWith(
        QueryJobConfiguration.Builder::class.java.typeName
      )
    }
    return if (configuratorIndex != -1) {
      parameters.removeAt(configuratorIndex)
      var positional = 0
      parameters.forEach {
        if (it.name.startsWith(":" + DataContextService.POSITIONAL_PARAMETER_NAME)) {
          it.name = ":${DataContextService.POSITIONAL_PARAMETER_NAME}${positional++}"
        }
      }
      configuratorIndex
    } else {
      -1
    }
  }
}