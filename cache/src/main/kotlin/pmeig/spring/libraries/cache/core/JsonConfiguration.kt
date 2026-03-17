package pmeig.spring.libraries.cache.core

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature

@Configuration
class JsonConfiguration: BeanPostProcessor {
  @Bean
  fun jsonMapperCustomizer(): JsonMapperBuilderCustomizer = {
    it.findAndAddModules()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .changeDefaultPropertyInclusion {
        propertyInclusion -> propertyInclusion.withContentInclusion(JsonInclude.Include.NON_NULL)
        .withValueInclusion(JsonInclude.Include.NON_NULL)
      }

  }
}