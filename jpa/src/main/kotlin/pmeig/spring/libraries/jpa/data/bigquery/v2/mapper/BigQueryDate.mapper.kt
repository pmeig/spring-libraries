package pmeig.spring.libraries.jpa.data.bigquery.v2.mapper

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.Range
import com.google.cloud.bigquery.StandardSQLTypeName
import org.threeten.extra.PeriodDuration
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperProvider
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

private class BigQueryDateMapper : BigQueryMapper<LocalDate> {
  private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  override fun map(value: FieldValue?): LocalDate? = value?.timestampInstant?.let { LocalDate.from(it) }
  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      it as? LocalDate
    }?.let { QueryParameterValue.date(dateFormat.format(it)) }
  }
}

private class BigQueryTimestampMapper : BigQueryMapper<Long> {
  override fun map(value: FieldValue?): Long? = value?.timestampValue
  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      if (it is Int) {
        it.toLong()
      } else it as? Long
    }?.let { QueryParameterValue.timestamp(it) }
  }
}

private class BigQueryInstantMapper : BigQueryMapper<Instant> {
  override fun map(value: FieldValue?): Instant? = value?.timestampInstant
  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      it as? Instant
    }?.let { QueryParameterValue.timestamp(it.toEpochMilli() * 1000) }
  }
}

private class BigQueryTimeMapper : BigQueryMapper<LocalTime> {
  override fun map(value: FieldValue?): LocalTime? = value?.timestampInstant?.let { LocalTime.from(it) }
  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      it as? LocalTime
    }?.let { QueryParameterValue.time("${it.hour}:${it.minute}:${it.second}") }
  }
}

private class BigQueryDateTimeMapper : BigQueryMapper<LocalDateTime> {
  private val dateFormat = DateTimeFormatter.ISO_DATE_TIME
  override fun map(value: FieldValue?): LocalDateTime? = value?.timestampInstant?.let { LocalDateTime.from(it) }
  override fun parameter(value: Any?): QueryParameterValue? = value?.let {
    it as? LocalDateTime
  }?.let { return QueryParameterValue.timestamp(dateFormat.format(it)) }
}

private class BigQueryPeriodMapper : BigQueryMapper<Period> {
  override fun map(value: FieldValue?): Period? = value?.periodDuration?.period
  override fun parameter(value: Any?): QueryParameterValue? = value?.let {
    it as? Period
  }?.let { QueryParameterValue.interval(PeriodDuration.of(it)) }
}

private class BigQueryDurationMapper : BigQueryMapper<Duration> {
  override fun map(value: FieldValue?): Duration? = value?.periodDuration?.duration
  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      it as? Duration
    }?.let { QueryParameterValue.interval(PeriodDuration.of(it)) }
  }
}

private abstract class BigQueryRangeMapper<T : Any>(
  private val timeMapper: BigQueryMapper<T>
) : BigQueryMapper<Pair<T, T>> {
  override fun map(value: FieldValue?): Pair<T, T>? = value?.rangeValue?.let {
    timeMapper.map(it.start)?.let { start ->
      timeMapper.map(it.end)?.let { end -> Pair(start, end) }
    }
  }

  override fun parameter(value: Any?): QueryParameterValue? {
    return value?.let {
      it as? Pair<*, *>
    }?.let {
      QueryParameterValue.range(Range.newBuilder().setStart(timeMapper.parameter(it.first)!!.value)
        .setEnd(timeMapper.parameter(it.second)!!.value).build())
    }
  }
}

private class BigQueryRangeDateMapper : BigQueryRangeMapper<LocalDate>(BigQueryDateMapper())
private class BigQueryRangeDateTimeMapper : BigQueryRangeMapper<LocalDateTime>(BigQueryDateTimeMapper())
private class BigQueryRangeInstantMapper : BigQueryRangeMapper<Instant>(BigQueryInstantMapper())
private class BigQueryRangeTimestampMapper : BigQueryRangeMapper<Long>(BigQueryTimestampMapper())

internal enum class BigQueryDate(
  private val types: Collection<StandardSQLTypeName>,
  private val target: KClass<*>,
  override val mapper: BigQueryMapper<*>
): BigQueryMapperProvider {
  DATE(StandardSQLTypeName.DATE, LocalDate::class, BigQueryDateMapper()),
  TIMESTAMP(StandardSQLTypeName.TIMESTAMP, Long::class, BigQueryTimestampMapper()),
  TIME(StandardSQLTypeName.TIME, LocalTime::class, BigQueryTimeMapper()),
  DATETIME(listOf(StandardSQLTypeName.DATETIME, StandardSQLTypeName.TIMESTAMP), LocalDateTime::class, BigQueryDateTimeMapper()),
  INSTANT(StandardSQLTypeName.TIMESTAMP, Instant::class, BigQueryInstantMapper()),
  PERIOD(StandardSQLTypeName.INTERVAL, Period::class, BigQueryPeriodMapper()),
  DURATION(StandardSQLTypeName.INTERVAL, Duration::class, BigQueryDurationMapper()),
  RANGE_DATETIME(StandardSQLTypeName.RANGE, LocalDateTime::class, BigQueryRangeDateTimeMapper()),
  RANGE(StandardSQLTypeName.RANGE, Long::class, BigQueryRangeTimestampMapper()),
  RANGE_DATE(StandardSQLTypeName.RANGE, LocalDate::class, BigQueryRangeDateMapper()),
  RANGE_INSTANT(StandardSQLTypeName.RANGE, Instant::class, BigQueryRangeInstantMapper());

  constructor(type: StandardSQLTypeName, target: KClass<*>, mapper: BigQueryMapper<*>): this(listOf(type), target, mapper)


  companion object {
    fun from(type: StandardSQLTypeName, target: KClass<*>? = null): BigQueryDate? {
      return target?.let { clazz -> entries.find { it.types.contains(type) && it.target == clazz } }
        ?: entries.find { it.types.contains(type) }
    }
  }
}