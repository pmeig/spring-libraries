package pmeig.spring.libraries.jpa.core.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import pmeig.spring.libraries.jpa.core.DATE_TIME_FORMATTER_PROVIDER
import pmeig.spring.libraries.jpa.core.ZONE_ID_PROVIDER
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime

@Converter(autoApply = true)
class InstantToTimestampConverter: AttributeConverter<Instant, Timestamp> {
  override fun convertToDatabaseColumn(attribute: Instant?): Timestamp? =
    attribute?.let { Timestamp.from(it) }

  override fun convertToEntityAttribute(dbData: Timestamp?): Instant? =
    dbData?.toInstant()
}

@Converter(autoApply = true)
class LocalDateToTimestampConverter: AttributeConverter<LocalDate, Timestamp> {
  override fun convertToDatabaseColumn(attribute: LocalDate?): Timestamp? =
    attribute?.let {
      val localDateTime = LocalDateTime.ofInstant(Instant.from(it), ZONE_ID_PROVIDER.getZoneId())
      Timestamp.valueOf(localDateTime)
    }

  override fun convertToEntityAttribute(dbData: Timestamp?): LocalDate? =
    dbData?.toInstant()?.let {
      LocalDate.ofInstant(it, ZONE_ID_PROVIDER.getZoneId())
    }
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

@Converter(autoApply = true)
class OffsetTimeToTimestampConverter: AttributeConverter<OffsetTime, Timestamp> {

  override fun convertToDatabaseColumn(attribute: OffsetTime?) =
    attribute?.atDate(LocalDate.now())?.let { Timestamp.valueOf(it.toLocalDateTime()) }

  override fun convertToEntityAttribute(dbData: Timestamp?) =
    dbData?.toLocalDateTime()?.let { OffsetTime.from(it) }
}

@Converter(autoApply = true)
class LocalTimeToTimestampConverter: AttributeConverter<LocalTime, Timestamp> {
  override fun convertToDatabaseColumn(attribute: LocalTime?) =
    attribute?.let { Timestamp.valueOf(LocalDateTime.of(LocalDate.now(), it)) }

  override fun convertToEntityAttribute(dbData: Timestamp?) =
    dbData?.toLocalDateTime()?.let { LocalTime.from(it) }
}