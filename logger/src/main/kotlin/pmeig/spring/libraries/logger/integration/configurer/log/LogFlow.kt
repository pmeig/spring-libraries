package pmeig.spring.libraries.logger.integration.configurer.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.context.annotation.Configuration
import org.springframework.integration.dsl.IntegrationFlowDefinition
import pmeig.spring.libraries.logger.integration.LoggerMessage
import pmeig.spring.libraries.logger.integration.configurer.LoggerFlowConfigurer
import pmeig.spring.libraries.logger.integration.configurer.log.argument.LoggerArgumentProvider
import pmeig.spring.libraries.logger.integration.configurer.log.mdc.MDCConfigurer
import java.util.regex.Pattern

@Configuration
class LogFlow(
  private val MDCConfigurers: List<MDCConfigurer>,
  private val loggerArgumentsProvider: List<LoggerArgumentProvider>
) : LoggerFlowConfigurer {
  private val loggers = mutableMapOf<String, Logger>()

  override fun configure(flow: IntegrationFlowDefinition<*>) {
    flow.handle { message: LoggerMessage ->
      val clearContext = prepareContext()
      logMessage(message)
      clearContext()
    }
  }

  private fun logMessage(message: LoggerMessage) {
    var builder = loggers.getOrPut(message.name) { LoggerFactory.getLogger(message.name) }.atLevel(message.level)
    message.marker?.let {
      builder = builder.addMarker(it)
    }
    message.cause?.let {
      builder = builder.setCause(it)
    }
    val log = prepareLog(message.message, builder, message.args)
    builder.log(log)
  }

  private fun prepareLog(message: String, builder: LoggingEventBuilder, arguments: Array<out Any?>): String {
    var log = message
    var index = 0
    val max = arguments.size
    PATTERN_ARGUMENT.matcher(message).results().map {
      val argument = it.group()
      argument.substring(1, argument.length - 1).trim()
    }.forEach {
      val argument = if (index < max) arguments[index] else ""
      var apply = { argument }
      if (it.isNotEmpty()) {
        log = log.replace("{$it}", "{}")
        apply = {
          loggerArgumentsProvider.stream().map { configurer -> Pair(argument, configurer.provide(it)) }
            .filter { result -> result.second != null }.findFirst().map { result ->

              result.second!! }.orElse(it)
        }
      } else index++
      builder.addArgument(apply)
    }
    return log
  }

  private fun prepareContext(): () -> Unit {
    var clearContext = {}
    MDCConfigurers.forEach { configurer ->
      val mdc = configurer.configure()
      mdc.forEach { (key, value) ->
        MDC.put(key, value)
        val previous = clearContext
        clearContext = {
          previous()
          MDC.remove(key)
        }
      }
    }
    return clearContext
  }
}

private val PATTERN_ARGUMENT = Pattern.compile("\\{[^}]*}")