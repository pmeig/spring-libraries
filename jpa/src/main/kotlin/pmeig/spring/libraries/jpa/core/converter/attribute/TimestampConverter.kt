package pmeig.spring.libraries.jpa.core.converter.attribute

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import pmeig.spring.libraries.jpa.core.DATE_TIME_FORMATTER_PROVIDER
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

@Converter(autoApply = true)
class InstantToTimestampConverter: AttributeConverter<Instant, Timestamp> {
  override fun convertToDatabaseColumn(attribute: Instant?): Timestamp? =
    attribute?.let { Timestamp.from(it) }

  override fun convertToEntityAttribute(dbData: Timestamp?): Instant? =
    dbData?.toInstant()
}

@Converter(autoApply = true)
class LocalDateTimeToTimestampConverter: AttributeConverter<LocalDateTime, Timestamp> {
  override fun convertToDatabaseColumn(attribute: LocalDateTime?): Timestamp? =
    attribute?.let { Timestamp.valueOf(it) }

  override fun convertToEntityAttribute(dbData: Timestamp?): LocalDateTime? =
    dbData?.toLocalDateTime()
}

@Converter(autoApply = true)
class ZonedDateTimeToTimestampConverter: AttributeConverter<ZonedDateTime, Timestamp> {
  override fun convertToDatabaseColumn(attribute: ZonedDateTime?): Timestamp? =
    attribute?.let { Timestamp.valueOf(it.toLocalDateTime()) }

  override fun convertToEntityAttribute(dbData: Timestamp?): ZonedDateTime? =
    dbData?.toLocalDateTime()?.let { ZonedDateTime.from(it) }
}

@Converter(autoApply = true)
class OffsetDateTimeToTimestampConverter: AttributeConverter<OffsetDateTime, Timestamp> {
  override fun convertToDatabaseColumn(attribute: OffsetDateTime?): Timestamp? =
    attribute?.let { Timestamp.valueOf(it.toLocalDateTime()) }

  override fun convertToEntityAttribute(dbData: Timestamp?): OffsetDateTime? =
    dbData?.toLocalDateTime()?.let {
      OffsetDateTime.from(it)
    }
}

@Converter(autoApply = true)
class StringToTimestampConverter: AttributeConverter<String, Timestamp> {
  override fun convertToDatabaseColumn(attribute: String?): Timestamp? =
    attribute?.let { Timestamp.valueOf(LocalDateTime.parse(it, DATE_TIME_FORMATTER_PROVIDER.dateTime())) }

  override fun convertToEntityAttribute(dbData: Timestamp?): String? =
    dbData?.toInstant()?.let { DATE_TIME_FORMATTER_PROVIDER.instant().format(it)}
}