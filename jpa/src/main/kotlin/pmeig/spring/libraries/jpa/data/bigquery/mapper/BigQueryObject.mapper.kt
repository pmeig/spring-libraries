package pmeig.spring.libraries.jpa.data.bigquery.mapper

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.gson.JsonObject
import org.springframework.util.ClassUtils
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
private fun arrayParameterConverter(value: Any?): QueryParameterValue? {
  if (null == value) return null
  var param: Array<*>? = if (ClassUtils.getUserClass(value).isArray) value as Array<*> else null
  if (null == param) {
    if (value is Collection<*>) {
      param = value.toTypedArray()
    } else if (value is Iterable<*>) {
      param = value.toList().toTypedArray()
    }
  }
  return param?.let {
    val type = ClassUtils.getUserClass(it).componentType as Class<Any>
    QueryParameterValue.array(it, type)
  }
}

private class BigQueryJsonMapper(private val jsonMapper: ObjectMapper): BigQueryMapper<Map<String, Any?>> {
  override fun map(value: FieldValue?): Map<String, Any?>? =
    value?.stringValue?.let { jsonMapper.convertValue(it, object: TypeReference<Map<String, Any?>>() {}) }

  override fun parameter(value: Any?): QueryParameterValue? = if (value is JsonObject) QueryParameterValue.json(value)
  else value?.let { QueryParameterValue.json(jsonMapper.writeValueAsString(it)) }
}

private abstract class BigQueryArrayMapper<T: MutableCollection<Any?>>(
  private val itemMapper: BigQueryMapper<*>,
  private val supplier: () -> T
): BigQueryMapper<T> {
  override fun map(value: FieldValue?): T? {
    val collection = supplier()
    var isNull = true
    value?.repeatedValue?.apply {
      isNull = false
      forEach {
        itemMapper.map(if (it.isNull) null else it)?.let { item -> collection.add(item) }
      }
    }
    return if (isNull) null else collection
  }

  @Suppress("UNCHECKED_CAST")
  override fun parameter(value: Any?): QueryParameterValue? = arrayParameterConverter(value)
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BigQueryArrayMapper<*>

    return itemMapper == other.itemMapper
  }

  override fun hashCode(): Int {
    return itemMapper.hashCode()
  }


}

private class BigQueryListMapper(itemMapper: BigQueryMapper<*>):
  BigQueryArrayMapper<MutableList<Any?>>(itemMapper, ::mutableListOf)

private class BigQuerySetMapper(itemMapper: BigQueryMapper<*>):
  BigQueryArrayMapper<MutableSet<Any?>>(itemMapper, ::mutableSetOf)

private class BigQueryTableMapper(private val itemMapper: BigQueryMapper<*>): BigQueryMapper<Array<Any?>> {
  override fun map(value: FieldValue?): Array<Any?>? {
    return value?.repeatedValue?.map { itemMapper.map(if (it.isNull) null else it) }?.toTypedArray()
  }

  override fun parameter(value: Any?): QueryParameterValue? = arrayParameterConverter(value)
}

private class BigQueryGeographyMapper(): BigQueryMapper<String> {
  override fun map(value: FieldValue?): String? {
    return value?.stringValue
  }

  override fun parameter(value: Any?): QueryParameterValue? = value?.let {
    QueryParameterValue.geography(value.toString())
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return true
  }

  override fun hashCode(): Int {
    return javaClass.hashCode()
  }


}

private class BigQueryStructMapper(private val mappers: Map<String, BigQueryMapper<*>>):
  BigQueryMapper<Map<String, Any?>> {
  override fun map(value: FieldValue?): Map<String, Any?>? {
    return value?.recordValue?.let { struct ->
      mappers.entries.associate { (key, mapper) ->
        key to mapper.map(struct[key].let {
          if (it.isNull) null else it
        })
      }
    }
  }

  override fun parameter(value: Any?): QueryParameterValue? = value?.let {
    val type = ClassUtils.getUserClass(it)
    QueryParameterValue
      .struct(mappers.entries.associate { (key, mapper) -> key to mapper.parameter(type.getField(key)[value]) })
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BigQueryStructMapper

    return mappers == other.mappers
  }

  override fun hashCode(): Int {
    return mappers.hashCode()
  }

}

internal enum class BigQueryObjectMapper(
  private val type: StandardSQLTypeName,
  private val target: Type,
  factory: (ObjectMapper, BigQueryMapper<*>?, Map<String, BigQueryMapper<*>>) -> BigQueryMapper<*>
) {
  JSON(StandardSQLTypeName.JSON, Map::class.javaObjectType, { mapper, _, _ ->
    BigQueryJsonMapper(mapper)
  }),
  LIST(StandardSQLTypeName.ARRAY, List::class, { itemMapper: BigQueryMapper<*> ->
    BigQueryListMapper(itemMapper)
  }),
  SET(StandardSQLTypeName.ARRAY, Set::class, { itemMapper: BigQueryMapper<*> ->
    BigQuerySetMapper(itemMapper)
  }),
  ARRAY(StandardSQLTypeName.ARRAY, Array::class, { itemMapper: BigQueryMapper<*> ->
    BigQueryTableMapper(itemMapper)
  }),
  GEOGRAPHY(StandardSQLTypeName.GEOGRAPHY, String::class.javaObjectType, { _, _, _ -> BigQueryGeographyMapper() }),
  STRUCT(StandardSQLTypeName.STRUCT, { mappers: Map<String, BigQueryMapper<*>> ->
    BigQueryStructMapper(mappers)
  }, Any::class);

  companion object {
    fun from(type: StandardSQLTypeName, target: KClass<*>): BigQueryObjectMapper? = from(type, target.javaObjectType)
    fun from(type: StandardSQLTypeName, target: Type? = null): BigQueryObjectMapper? =
      target?.let { clazzType ->
        val clazz = (if (clazzType is ParameterizedType) clazzType.rawType else clazzType) as Class<*>
        entries.find { it.type == type && (it.target as Class<*>).isAssignableFrom(clazz) }
      }
        ?: entries.find { it.type == type }

    fun from(target: Type): BigQueryObjectMapper? {
      val clazz = (if (target is ParameterizedType)
        target.rawType
      else
        target) as Class<*>
      return entries.find { (it.target as Class<*>).isAssignableFrom(clazz) }
    }
  }

  var factory: (ObjectMapper, BigQueryMapper<*>?, Map<String, BigQueryMapper<*>>) -> BigQueryMapper<*> =
    { _, _, _ -> error("Factory not initialized") }
    private set

  init {
    this.factory = if (listOf(StandardSQLTypeName.JSON, StandardSQLTypeName.GEOGRAPHY).contains(type)) {
      val builder = factory
      { mapper, itemMapper, struct ->
        val mapper = builder(mapper, itemMapper, struct)
        this.factory = { _, _, _ -> mapper }
        mapper
      }
    } else factory
  }

  constructor(
    type: StandardSQLTypeName, target: KClass<*>,
    factory: (BigQueryMapper<*>) -> BigQueryMapper<*>
  ): this(type, target.javaObjectType, { _, itemMapper, _ -> factory(itemMapper!!) })

  constructor(
    type: StandardSQLTypeName,
    factory: (Map<String, BigQueryMapper<*>>) -> BigQueryMapper<*>, target: KClass<*>
  ): this(type, target.javaObjectType, { _, _, struct -> factory(struct) })
}