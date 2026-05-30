package pmeig.spring.libraries.jpa.core.converter.attribute

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import pmeig.spring.libraries.jpa.core.DATE_TIME_FORMATTER_PROVIDER
import java.sql.Time
import java.time.LocalTime
import java.time.OffsetTime

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
class OffsetTimeToTimeConverter: AttributeConverter<OffsetTime, Time> {
  override fun convertToDatabaseColumn(attribute: OffsetTime?) = attribute?.let { Time.valueOf(it.toLocalTime()) }

  override fun convertToEntityAttribute(dbData: Time?) = dbData?.toLocalTime()?.let { OffsetTime.from(it) }
}