package pmeig.spring.libraries.logger.integration.configurer.log.mdc

interface MDCConfigurer {
  fun configure() = emptyMap<String, String>()
}