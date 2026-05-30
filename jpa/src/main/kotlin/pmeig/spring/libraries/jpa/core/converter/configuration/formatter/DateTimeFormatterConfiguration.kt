package pmeig.spring.libraries.jpa.core.converter.configuration.formatter

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.jpa.core.dateTimeFormatterInstance
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

@Configuration
class DateTimeFormatterConfiguration: BeanPostProcessor {
  private var saveFormatterProvider: (bean: Any) -> Any = { bean ->
    if (bean is DateTimeFormatterProvider) {
      dateTimeFormatterInstance = bean
      saveFormatterProvider = { it }
    }
    bean
  }

  @ConditionalOnMissingBean(DateTimeFormatterProvider::class)
  @Bean
  fun defaultFormatterProvider(): DateTimeFormatterProvider {
    val date = DateTimeFormatter.ISO_DATE
    val time = DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ISO_TIME)
      .optionalStart()
      .appendLiteral('[')
      .parseCaseSensitive()
      .appendZoneRegionId()
      .appendLiteral(']')
      .toFormatter()
    val dateTime = DateTimeFormatter.ISO_DATE_TIME
    val instant = DateTimeFormatter.ISO_INSTANT
    return object : DateTimeFormatterProvider {
      override fun date() = date
      override fun time() = time
      override fun dateTime() = dateTime
      override fun instant() = instant
    }
  }

  override fun postProcessAfterInitialization(bean: Any, beanName: String) = saveFormatterProvider(bean)
}