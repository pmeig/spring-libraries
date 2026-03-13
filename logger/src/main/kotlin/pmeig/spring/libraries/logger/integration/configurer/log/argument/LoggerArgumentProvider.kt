package pmeig.spring.libraries.logger.integration.configurer.log.argument

interface LoggerArgumentProvider {
  fun provide(argument: String): String? = null
}