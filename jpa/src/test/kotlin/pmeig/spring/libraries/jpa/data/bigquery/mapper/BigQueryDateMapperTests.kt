package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.Range
import com.google.cloud.bigquery.StandardSQLTypeName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.threeten.extra.PeriodDuration
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period

class BigQueryDateMapperTest {

  @Nested
  inner class FromMethod {

    @Nested
    inner class WithTypeAndKClass {
      @Test
      fun `returns DATE mapper for LocalDate`() {
        val result = BigQueryDate.from(StandardSQLTypeName.DATE, LocalDate::class)
        assertEquals(BigQueryDate.DATE, result)
      }

      @Test
      fun `returns TIMESTAMP mapper for Long`() {
        val result = BigQueryDate.from(StandardSQLTypeName.TIMESTAMP, Long::class)
        assertEquals(BigQueryDate.TIMESTAMP, result)
      }

      @Test
      fun `returns TIME mapper for LocalTime`() {
        val result = BigQueryDate.from(StandardSQLTypeName.TIME, LocalTime::class)
        assertEquals(BigQueryDate.TIME, result)
      }

      @Test
      fun `returns DATETIME mapper for LocalDateTime`() {
        val result = BigQueryDate.from(StandardSQLTypeName.DATETIME, LocalDateTime::class)
        assertEquals(BigQueryDate.DATETIME, result)
      }

      @Test
      fun `returns INSTANT mapper for Instant`() {
        val result = BigQueryDate.from(StandardSQLTypeName.TIMESTAMP, Instant::class)
        assertEquals(BigQueryDate.INSTANT, result)
      }

      @Test
      fun `returns PERIOD mapper for Period`() {
        val result = BigQueryDate.from(StandardSQLTypeName.INTERVAL, Period::class)
        assertEquals(BigQueryDate.PERIOD, result)
      }

      @Test
      fun `returns DURATION mapper for Duration`() {
        val result = BigQueryDate.from(StandardSQLTypeName.INTERVAL, Duration::class)
        assertEquals(BigQueryDate.DURATION, result)
      }

      @Test
      fun `returns RANGE_DATE mapper for LocalDate range`() {
        val result = BigQueryDate.from(StandardSQLTypeName.RANGE, LocalDate::class)
        assertEquals(BigQueryDate.RANGE_DATE, result)
      }

      @Test
      fun `returns RANGE_DATETIME mapper for LocalDateTime range`() {
        val result = BigQueryDate.from(StandardSQLTypeName.RANGE, LocalDateTime::class)
        assertEquals(BigQueryDate.RANGE_DATETIME, result)
      }

      @Test
      fun `returns RANGE_INSTANT mapper for Instant range`() {
        val result = BigQueryDate.from(StandardSQLTypeName.RANGE, Instant::class)
        assertEquals(BigQueryDate.RANGE_INSTANT, result)
      }

      @Test
      fun `returns RANGE mapper for Long range`() {
        val result = BigQueryDate.from(StandardSQLTypeName.RANGE, Long::class)
        assertEquals(BigQueryDate.RANGE, result)
      }
    }

    @Nested
    inner class WithTypeAndType {
      @Test
      fun `returns DATE mapper for LocalDate type`() {
        val result = BigQueryDate.from(StandardSQLTypeName.DATE, LocalDate::class.javaObjectType)
        assertEquals(BigQueryDate.DATE, result)
      }

      @Test
      fun `returns null when no match found`() {
        val result = BigQueryDate.from(StandardSQLTypeName.STRING, String::class.javaObjectType)
        assertNull(result)
      }

      @Test
      fun `returns first matching mapper when target is null`() {
        val result = BigQueryDate.from(StandardSQLTypeName.TIMESTAMP)
        assertNotNull(result)
      }
    }

    @Nested
    inner class WithTypeOnly {
      @Test
      fun `returns LocalDate mapper for target type`() {
        val result = BigQueryDate.from(LocalDate::class.javaObjectType)
        assertEquals(BigQueryDate.DATE, result)
      }

      @Test
      fun `returns LocalDateTime mapper for target type`() {
        val result = BigQueryDate.from(LocalDateTime::class.javaObjectType)
        assertEquals(BigQueryDate.DATETIME, result)
      }

      @Test
      fun `returns null when no match for target type`() {
        val result = BigQueryDate.from(String::class.javaObjectType)
        assertNull(result)
      }
    }
  }

  @Nested
  inner class BigQueryDateMapper {

    private val mapper = BigQueryDate.DATE.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `parses date string to LocalDate`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("2024-01-15")

        val result = mapper.map(fieldValue)

        assertEquals(LocalDate.of(2024, 1, 15), result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts LocalDate to QueryParameterValue`() {
        val date = LocalDate.of(2024, 1, 15)

        val result = mapper.parameter(date)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not LocalDate`() {
        assertNull(mapper.parameter("not-a-date"))
      }
    }
  }

  @Nested
  inner class BigQueryTimestampMapper {

    private val mapper = BigQueryDate.TIMESTAMP.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts timestamp value`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.timestampValue).thenReturn(1234567890L)

        val result = mapper.map(fieldValue)

        assertEquals(1234567890L, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Long to QueryParameterValue`() {
        val result = mapper.parameter(1234567890L)

        assertNotNull(result)
      }

      @Test
      fun `converts Int to Long and creates QueryParameterValue`() {
        val result = mapper.parameter(123456)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not Int or Long`() {
        assertNull(mapper.parameter("not-a-number"))
      }
    }
  }

  @Nested
  inner class BigQueryInstantMapper {

    private val mapper = BigQueryDate.INSTANT.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts Instant from timestamp`() {
        val instant = Instant.now()
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.timestampInstant).thenReturn(instant)

        val result = mapper.map(fieldValue)

        assertEquals(instant, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Instant to QueryParameterValue with milliseconds`() {
        val instant = Instant.ofEpochMilli(1234567890)

        val result = mapper.parameter(instant)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not Instant`() {
        assertNull(mapper.parameter("not-an-instant"))
      }
    }
  }

  @Nested
  inner class BigQueryTimeMapper {

    private val mapper = BigQueryDate.TIME.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `parses time string to LocalTime`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("14:30:45.123")

        val result = mapper.map(fieldValue)

        assertEquals(LocalTime.of(14, 30, 45, 123_000_000), result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts LocalTime to QueryParameterValue`() {
        val time = LocalTime.of(14, 30, 45)

        val result = mapper.parameter(time)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not LocalTime`() {
        assertNull(mapper.parameter("not-a-time"))
      }
    }
  }

  @Nested
  inner class BigQueryDateTimeMapper {

    private val mapper = BigQueryDate.DATETIME.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `parses ISO datetime string to LocalDateTime`() {
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.stringValue).thenReturn("2024-01-15T14:30:45")

        val result = mapper.map(fieldValue)

        assertEquals(LocalDateTime.of(2024, 1, 15, 14, 30, 45), result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts LocalDateTime to QueryParameterValue`() {
        val dateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45)

        val result = mapper.parameter(dateTime)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not LocalDateTime`() {
        assertNull(mapper.parameter("not-a-datetime"))
      }
    }
  }

  @Nested
  inner class BigQueryPeriodMapper {

    private val mapper = BigQueryDate.PERIOD.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts Period from PeriodDuration`() {
        val period = Period.ofDays(5)
        val periodDuration = PeriodDuration.of(period)
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.periodDuration).thenReturn(periodDuration)

        val result = mapper.map(fieldValue)

        assertEquals(period, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Period to QueryParameterValue`() {
        val period = Period.ofDays(5)

        val result = mapper.parameter(period)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not Period`() {
        assertNull(mapper.parameter("not-a-period"))
      }
    }
  }

  @Nested
  inner class BigQueryDurationMapper {

    private val mapper = BigQueryDate.DURATION.mapper

    @Nested
    inner class MapMethod {
      @Test
      fun `returns null when FieldValue is null`() {
        assertNull(mapper.map(null))
      }

      @Test
      fun `extracts Duration from PeriodDuration`() {
        val duration = Duration.ofHours(3)
        val periodDuration = PeriodDuration.of(duration)
        val fieldValue = mock<FieldValue>()
        whenever(fieldValue.periodDuration).thenReturn(periodDuration)

        val result = mapper.map(fieldValue)

        assertEquals(duration, result)
      }
    }

    @Nested
    inner class ParameterMethod {
      @Test
      fun `returns null when value is null`() {
        assertNull(mapper.parameter(null))
      }

      @Test
      fun `converts Duration to QueryParameterValue`() {
        val duration = Duration.ofHours(3)

        val result = mapper.parameter(duration)

        assertNotNull(result)
      }

      @Test
      fun `returns null when value is not Duration`() {
        assertNull(mapper.parameter("not-a-duration"))
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  @Nested
  inner class BigQueryRangeMapper {

    @Nested
    inner class BigQueryRangeDateMapper {

      private val mapper = BigQueryDate.RANGE_DATE.mapper as BigQueryMapper<Pair<LocalDate, LocalDate>>

      @Nested
      inner class MapMethod {
        @Test
        fun `returns null when FieldValue is null`() {
          assertNull(mapper.map(null))
        }

        @Test
        fun `extracts date range as Pair`() {
          val startDate = "2024-01-01"
          val endDate = "2024-01-31"

          val startValue = mock<FieldValue>()
          whenever(startValue.stringValue).thenReturn(startDate)

          val endValue = mock<FieldValue>()
          whenever(endValue.stringValue).thenReturn(endDate)

          val range = mock<Range>()
          whenever(range.start).thenReturn(startValue)
          whenever(range.end).thenReturn(endValue)

          val fieldValue = mock<FieldValue>()
          whenever(fieldValue.rangeValue).thenReturn(range)

          val result = mapper.map(fieldValue)

          assertNotNull(result)
          assertEquals(LocalDate.of(2024, 1, 1), result?.first)
          assertEquals(LocalDate.of(2024, 1, 31), result?.second)
        }

        @Test
        fun `returns null when start is null`() {
          val endValue = mock<FieldValue>()
          whenever(endValue.stringValue).thenReturn("2024-01-31")

          val range = mock<Range>()
          whenever(range.start).thenReturn(null)
          whenever(range.end).thenReturn(endValue)

          val fieldValue = mock<FieldValue>()
          whenever(fieldValue.rangeValue).thenReturn(range)

          assertNull(mapper.map(fieldValue))
        }
      }

      @Nested
      inner class ParameterMethod {
        @Test
        fun `returns null when value is null`() {
          assertNull(mapper.parameter(null))
        }

        @Test
        fun `converts Pair of LocalDate to QueryParameterValue`() {
          val range = Pair(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))

          val result = mapper.parameter(range)

          assertNotNull(result)
        }

        @Test
        fun `returns null when value is not Pair`() {
          assertNull(mapper.parameter("not-a-pair"))
        }
      }
    }
    @Nested
    inner class BigQueryRangeDateTimeMapper {

      private val mapper = BigQueryDate.RANGE_DATETIME.mapper as BigQueryMapper<Pair<LocalDateTime, LocalDateTime>>

      @Nested
      inner class MapMethod {
        @Test
        fun `returns null when FieldValue is null`() {
          assertNull(mapper.map(null))
        }

        @Test
        fun `extracts datetime range as Pair`() {
          val startDateTime = "2024-01-01T00:00:00"
          val endDateTime = "2024-01-31T23:59:59"

          val startValue = mock<FieldValue>()
          whenever(startValue.stringValue).thenReturn(startDateTime)

          val endValue = mock<FieldValue>()
          whenever(endValue.stringValue).thenReturn(endDateTime)

          val range = mock<Range>()
          whenever(range.start).thenReturn(startValue)
          whenever(range.end).thenReturn(endValue)

          val fieldValue = mock<FieldValue>()
          whenever(fieldValue.rangeValue).thenReturn(range)

          val result = mapper.map(fieldValue)

          assertNotNull(result)
          assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0, 0), result!!.first)
          assertEquals(LocalDateTime.of(2024, 1, 31, 23, 59, 59), result.second)
        }
      }

      @Nested
      inner class ParameterMethod {
        @Test
        fun `converts Pair of LocalDateTime to QueryParameterValue`() {
          val range = Pair(
            LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 1, 31, 23, 59, 59)
          )

          val result = mapper.parameter(range)

          assertNotNull(result)
        }
      }
    }
    @Nested
    inner class BigQueryRangeInstantMapper {

      private val mapper = BigQueryDate.RANGE_INSTANT.mapper as BigQueryMapper<Pair<Instant, Instant>>

      @Nested
      inner class MapMethod {
        @Test
        fun `returns null when FieldValue is null`() {
          assertNull(mapper.map(null))
        }

        @Test
        fun `extracts instant range as Pair`() {
          val start = Instant.ofEpochMilli(1000000)
          val end = Instant.ofEpochMilli(2000000)

          val startValue = mock<FieldValue>()
          whenever(startValue.timestampInstant).thenReturn(start)

          val endValue = mock<FieldValue>()
          whenever(endValue.timestampInstant).thenReturn(end)

          val range = mock<Range>()
          whenever(range.start).thenReturn(startValue)
          whenever(range.end).thenReturn(endValue)

          val fieldValue = mock<FieldValue>()
          whenever(fieldValue.rangeValue).thenReturn(range)

          val result = mapper.map(fieldValue)

          assertNotNull(result)
          assertEquals(start, result!!.first)
          assertEquals(end, result.second)
        }
      }

      @Nested
      inner class ParameterMethod {
        @Test
        fun `converts Pair of Instant to QueryParameterValue`() {
          val range = Pair(
            Instant.ofEpochMilli(1000000),
            Instant.ofEpochMilli(2000000)
          )

          val result = mapper.parameter(range)

          assertNotNull(result)
        }
      }
    }
    @Nested
    inner class BigQueryRangeTimestampMapper {

      private val mapper = BigQueryDate.RANGE.mapper as BigQueryMapper<Pair<Long, Long>>

      @Nested
      inner class MapMethod {
        @Test
        fun `returns null when FieldValue is null`() {
          assertNull(mapper.map(null))
        }

        @Test
        fun `extracts timestamp range as Pair`() {
          val startValue = mock<FieldValue>()
          whenever(startValue.timestampValue).thenReturn(1000000L)

          val endValue = mock<FieldValue>()
          whenever(endValue.timestampValue).thenReturn(2000000L)

          val range = mock<Range>()
          whenever(range.start).thenReturn(startValue)
          whenever(range.end).thenReturn(endValue)

          val fieldValue = mock<FieldValue>()
          whenever(fieldValue.rangeValue).thenReturn(range)

          val result = mapper.map(fieldValue)

          assertNotNull(result)
          assertEquals(1000000L, result!!.first)
          assertEquals(2000000L, result.second)
        }
      }

      @Nested
      inner class ParameterMethod {
        @Test
        fun `converts Pair of Long to QueryParameterValue`() {
          val range = Pair(1000000L, 2000000L)

          val result = mapper.parameter(range)

          assertNotNull(result)
        }
      }
    }
  }
}
