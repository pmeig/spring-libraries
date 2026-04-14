package pmeig.spring.libraries.jpa.shared.helper

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName

fun createField(name: String, type: LegacySQLTypeName, vararg fields: Field) = fieldBuilder(
  name,
  type,
  *fields
).build()

fun fieldBuilder(name: String, type: LegacySQLTypeName, vararg fields: Field) = Field.newBuilder(name, type, *fields)