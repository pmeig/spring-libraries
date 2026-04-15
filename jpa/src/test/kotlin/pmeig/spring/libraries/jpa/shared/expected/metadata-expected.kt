package pmeig.spring.libraries.jpa.shared.expected

import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.FieldAccessorWrapper
import pmeig.spring.libraries.jpa.core.ParentFieldAccessor
import pmeig.spring.libraries.jpa.core.column.id.auto.AutoIdStateColumns
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.core.entity.model.DataPrimaryMetadata
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryStructType
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import pmeig.spring.libraries.jpa.shared.model.StructColumn
import java.lang.reflect.Field

private val ENTITY_TEST_METADATA = EntityTest::class.qualifiedName + "_metadata"
private val ARRAY_METADATA_FACTORY = "arrayMetadataFactory_expected"

private val cache = mutableMapOf<String, Any>()

fun entityTestMetadata_expected() = getCache(ENTITY_TEST_METADATA) {
  val clazz = EntityTest::class.javaObjectType
  val constructor = clazz.declaredConstructors.find { it.parameterCount == 0 }!!
  DataMetadata(
    "entity_test",
    generateEntityTestDataPrimary(),
    generateEntityTestColumn(clazz)
  ) {
    constructor.newInstance()
  }
}

fun arrayMetadataFactory_expected(): BigQueryMetadataFactory = getCache(ARRAY_METADATA_FACTORY) {
  BigQueryMetadataFactory(List::class.java, String::class.java)
}

fun generateEntityTestColumn(clazz: Class<EntityTest>): Map<String, FieldAccessor<Any>> {
  val columns = mutableMapOf<String, FieldAccessor<Any>>()
  clazz.declaredFields.filter { !listOf("struct", "secretName").contains(it.name) }.forEach {
    columns[it.name] = getFieldByCache(it)
  }
  columns["secret"] = getFieldByCache(clazz.getDeclaredField("secretName"))
  columns["struct"] = generateStructAccessor(clazz)
  return columns + generateColumnSuperclass(clazz.superclass)
}

fun generateArrayAccessor(clazz: Class<EntityTest> = EntityTest::class.javaObjectType): FieldAccessor<*> = getFieldByCache(clazz.getDeclaredField("array"))

fun generateStructAccessor(clazz: Class<EntityTest> = EntityTest::class.javaObjectType): FieldAccessorWrapper<Any> =
  clazz.getDeclaredField("struct").let { field ->
    getCache(generateKeyField(field)) {
      FieldAccessorWrapper(field, StructColumn::class.javaObjectType.declaredFields.associate { structField ->
        structField.name to getFieldByCache(structField) { accessor ->
          ParentFieldAccessor(field, accessor)
        }
      })
    }
  }

fun structMetadataFactory_expected(): BigQueryMetadataFactory =
  getCache(generateKeyMetadataFactory("struct")) {
    BigQueryMetadataFactory(
      Map::class.javaObjectType,
      null,
      BigQueryStructType(
        StructColumn::class.javaObjectType.declaredFields.associate {
          it.name to getCache(generateKeyMetadataFactory("${StructColumn::class.javaObjectType.typeName}_${it}")) {
            BigQueryMetadataFactory(it.type)
          }
        }
      )
    )
  }

fun generateKeyMetadataFactory(name: String): String = "${name}_metadataFactory"

fun generateColumnSuperclass(clazz: Class<*>): Map<String, FieldAccessor<Any>> = if (clazz == Any::class.javaObjectType)
  emptyMap()
else
  clazz.declaredFields.associate {
    it.name.replace("By", "_by") to FieldAccessorWrapper<Any>(it)
  } + generateColumnSuperclass(clazz.superclass)

fun generateEntityTestDataPrimary() = AutoIdStateColumns::class.javaObjectType.getDeclaredField("id").let {
  DataPrimaryMetadata(
    it, mapOf("id" to getFieldByCache(it))
  )
}

private fun getFieldByCache(
  field: Field,
  transform: (FieldAccessor<Any>) -> FieldAccessor<Any> = { it }
): FieldAccessor<Any> = getCache(generateKeyField(field)) {
  transform(FieldAccessorWrapper(field))
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> getCache(key: String, defaultValue: () -> T) = cache.getOrPut(key) {
  defaultValue() as Any
} as T

private fun generateKeyField(field: Field) = "${field.declaringClass.typeName}_field_${field.name}"