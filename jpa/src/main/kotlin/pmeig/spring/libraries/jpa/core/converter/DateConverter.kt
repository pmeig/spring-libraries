package pmeig.spring.libraries.jpa.core.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import pmeig.spring.libraries.jpa.core.DATE_TIME_FORMATTER_PROVIDER
import java.sql.Date
import java.time.Instant
import java.time.LocalDate


@Converter(autoApply = true)
class LocalDateToDateConverter: AttributeConverter<LocalDate, Date> {
  override fun convertToDatabaseColumn(attribute: LocalDate?): Date? = attribute?.let { Date.valueOf(it) }

  override fun convertToEntityAttribute(dbData: Date?): LocalDate? = dbData?.toLocalDate()
}

@Converter(autoApply = true)
class InstantToDateConverter: AttributeConverter<Instant, Date> {
  override fun convertToDatabaseColumn(attribute: Instant?): Date? =
    attribute?.let { Date.valueOf(LocalDate.from(it)) }

  override fun convertToEntityAttribute(dbData: Date?): Instant? = dbData?.toInstant()
}

@Converter(autoApply = true)
class StringToDateConverter: AttributeConverter<String, Date> {
  override fun convertToDatabaseColumn(attribute: String?): Date? =
    attribute?.let { Date.valueOf(LocalDate.parse(it, DATE_TIME_FORMATTER_PROVIDER.date())) }

  override fun convertToEntityAttribute(dbData: Date?): String? =
    dbData?.toLocalDate()?.let { DATE_TIME_FORMATTER_PROVIDER.date().format(it) }
}

