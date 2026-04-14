package pmeig.spring.libraries.jpa.shared.helper

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema

private val cache = mutableMapOf<String, Any>()

fun struct_field() = getField("struct_field") {
  createField("struct", LegacySQLTypeName.RECORD,
    createField("name", LegacySQLTypeName.STRING),
    createField("age", LegacySQLTypeName.INTEGER),
    createField("size", LegacySQLTypeName.INTEGER)
  )
}

fun struct_result() = getField("struct_result") {
  FieldValue.of(FieldValue.Attribute.RECORD, FieldValueList.of(listOf(
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "name"),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2"),
  ), struct_field().subFields))
}

fun array_field() = getField("array_field") {
  fieldBuilder("array", LegacySQLTypeName.STRING).setMode(Field.Mode.REPEATED).build()
}

fun array_result() = getField("array_result") {
  FieldValue.of(FieldValue.Attribute.REPEATED, FieldValueList.of(listOf(
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "first"),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "second")
  )))
}

fun entityTest_fields() = getField("entity_test") {
  FieldList.of(createField("id", LegacySQLTypeName.INTEGER),
    createField("created_by", LegacySQLTypeName.STRING),
    createField("created", LegacySQLTypeName.DATETIME),
    createField("updated_by", LegacySQLTypeName.STRING),
    createField("updated", LegacySQLTypeName.DATETIME),
    struct_field(),
    createField("secret", LegacySQLTypeName.STRING),
    array_field()
    )
}

fun entityTest_result() = getField("entity_test_result") {
  FieldValue.of(FieldValue.Attribute.RECORD, FieldValueList.of(listOf(
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1"),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "created_by"),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2020-01-01T00:00:00.000Z"),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "updated_by"),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2020-01-02T00:00:00.000Z"),
    struct_result(),
    FieldValue.of(FieldValue.Attribute.PRIMITIVE, "secret"),
    array_result()
  )))
}

fun entityTest_schema() = getField("entity_test_schema") {
  Schema.of(entityTest_fields())
}


@Suppress("UNCHECKED_CAST")
private fun <T: Any> getField(key: String, creator: () -> T) = cache.getOrPut(key, creator) as T