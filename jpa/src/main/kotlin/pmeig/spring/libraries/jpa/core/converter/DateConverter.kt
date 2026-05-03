package pmeig.spring.libraries.jpa.core.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import pmeig.spring.libraries.jpa.core.DATE_TIME_FORMATTER_PROVIDER
import pmeig.spring.libraries.jpa.core.ZONE_ID_PROVIDER
import java.sql.Date
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime


@Converter(autoApply = true)
class LocalDateToDateConverter: AttributeConverter<LocalDate, Date> {
  override fun convertToDatabaseColumn(attribute: LocalDate?): Date? = attribute?.let { Date.valueOf(it) }

  override fun convertToEntityAttribute(dbData: Date?): LocalDate? = dbData?.toLocalDate()
}

@Converter(autoApply = true)
class OffsetDateToDateTimeConverter: AttributeConverter<OffsetDateTime, Date> {
  override fun convertToDatabaseColumn(attribute: OffsetDateTime?): Date? = attribute?.let { Date.valueOf(it.toLocalDate()) }

  override fun convertToEntityAttribute(dbData: Date?): OffsetDateTime? =
    dbData?.toInstant()?.let {
      OffsetDateTime.from(LocalDateTime.ofInstant(it, ZONE_ID_PROVIDER.getZoneId()))
    }
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

@Converter(autoApply = true)
class ZonedDateTimeToDateConverter: AttributeConverter<ZonedDateTime, Date> {

  override fun convertToDatabaseColumn(attribute: ZonedDateTime?): Date? = attribute?.let { Date.valueOf(it.toLocalDate()) }

  override fun convertToEntityAttribute(dbData: Date?): ZonedDateTime? =
    dbData?.toInstant()?.let {
      ZonedDateTime.ofInstant(it, ZONE_ID_PROVIDER.getZoneId())
    }
}

@Converter(autoApply = true)
class LocalDateTimeToDateConverter: AttributeConverter<LocalDateTime, Date> {
  override fun convertToDatabaseColumn(attribute: LocalDateTime?): Date? =
    attribute?.let { Date.valueOf(it.toLocalDate()) }

  override fun convertToEntityAttribute(dbData: Date?): LocalDateTime? =
    dbData?.toInstant()?.let {
      LocalDateTime.ofInstant(it, ZONE_ID_PROVIDER.getZoneId())
    }
}

