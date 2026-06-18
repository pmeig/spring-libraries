package pmeig.spring.libraries.jpa.shared.expected

import pmeig.spring.libraries.jpa.shared.model.EntityTest
import pmeig.spring.libraries.jpa.shared.model.StructColumn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun entity_test_expected() = EntityTest(
  1, "created_by",
  LocalDateTime.parse("2020-01-01T00:00:00.000Z", DateTimeFormatter.ISO_DATE_TIME),
  "updated_by",
  LocalDateTime.parse("2020-01-02T00:00:00.000Z", DateTimeFormatter.ISO_DATE_TIME),
  StructColumn("name", 1, 2),
  "secret",
  null,
  listOf("first", "second")
)