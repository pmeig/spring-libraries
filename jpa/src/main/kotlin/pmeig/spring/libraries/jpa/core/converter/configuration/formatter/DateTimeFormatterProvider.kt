package pmeig.spring.libraries.jpa.core.converter.configuration.formatter

import java.time.format.DateTimeFormatter

interface DateTimeFormatterProvider {
  fun date(): DateTimeFormatter
  fun time(): DateTimeFormatter
  fun dateTime(): DateTimeFormatter
  fun instant(): DateTimeFormatter
}