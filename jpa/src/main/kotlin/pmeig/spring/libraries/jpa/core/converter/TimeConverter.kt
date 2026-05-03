package pmeig.spring.libraries.jpa.core.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import pmeig.spring.libraries.jpa.core.DATE_TIME_FORMATTER_PROVIDER
import pmeig.spring.libraries.jpa.core.ZONE_ID_PROVIDER
import java.sql.Time
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime

@Converter(autoApply = true)
class ZonedDateTimeToTimeConverter: AttributeConverter<ZonedDateTime, Time> {
  override fun convertToDatabaseColumn(attribute: ZonedDateTime?): Time? =
    attribute?.let { Time.valueOf(it.toLocalTime()) }

  override fun convertToEntityAttribute(dbData: Time?): ZonedDateTime? = dbData?.toInstant()?.let {
    ZonedDateTime.ofInstant(it,
      ZONE_ID_PROVIDER.getZoneId()
    )
  }
}

@Converter(autoApply = true)
class LocalTimeToTimeConverter: AttributeConverter<LocalTime, Time> {
  override fun convertToDatabaseColumn(attribute: LocalTime?): Time? = attribute?.let { Time.valueOf(it) }
  override fun convertToEntityAttribute(dbData: Time?): LocalTime? = dbData?.toLocalTime()
}

@Converter(autoApply = true)
class StringToTimeConverter: AttributeConverter<String, Time> {
  override fun convertToDatabaseColumn(attribute: String?): Time? =
    attribute?.let { Time.valueOf(LocalTime.parse(it, DATE_TIME_FORMATTER_PROVIDER.time())) }

  override fun convertToEntityAttribute(dbData: Time?): String? =
    dbData?.toLocalTime()?.let { DATE_TIME_FORMATTER_PROVIDER.time().format(it) }
}

@Converter(autoApply = true)
class LocalDateTimeToTimeConverter: AttributeConverter<LocalDateTime, Time> {
  override fun convertToDatabaseColumn(attribute: LocalDateTime?): Time? =
    attribute?.let { Time.valueOf(it.toLocalTime()) }

  override fun convertToEntityAttribute(dbData: Time?): LocalDateTime? =
    dbData?.toLocalTime()?.let { LocalDateTime.from(it) }
}

@Converter(autoApply = true)
class OffsetTimeToTimeConverter: AttributeConverter<OffsetTime, Time> {
  override fun convertToDatabaseColumn(attribute: OffsetTime?) = attribute?.let { Time.valueOf(it.toLocalTime()) }

  override fun convertToEntityAttribute(dbData: Time?) = dbData?.toLocalTime()?.let { OffsetTime.from(it) }
}

@Converter(autoApply = true)
class OffsetDateTimeToTimeConverter: AttributeConverter<OffsetDateTime, Time> {
  override fun convertToDatabaseColumn(attribute: OffsetDateTime?) = attribute?.let { Time.valueOf(it.toLocalTime()) }

  override fun convertToEntityAttribute(dbData: Time?) = dbData?.toInstant()?.let {
    val localDateTime = LocalDateTime.ofInstant(it, ZONE_ID_PROVIDER.getZoneId())
    OffsetDateTime.of(localDateTime, ZONE_ID_PROVIDER.getZoneId().rules.getOffset(localDateTime))
  }
}