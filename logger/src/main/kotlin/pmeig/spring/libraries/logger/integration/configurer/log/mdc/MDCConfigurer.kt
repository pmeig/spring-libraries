package pmeig.spring.libraries.logger.integration.configurer.log.mdc

fun interface MDCConfigurer {
  fun configure(): Map<String, String>
}