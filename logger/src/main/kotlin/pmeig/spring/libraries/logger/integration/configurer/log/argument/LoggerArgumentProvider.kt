package pmeig.spring.libraries.logger.integration.configurer.log.argument

fun interface LoggerArgumentProvider {
  fun provide(argument: String): String?
}