package pmeig.spring.libraries.jpa.data.bigquery.converter.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQueryPrimaryKey
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQuerySQLMetadata
import java.util.regex.Pattern
import kotlin.reflect.KClass

@Component
class BigQueryAnnotationConverter : Converter<Any, BigQuerySQLMetadata> {

  private val upperCaseCatcher = Pattern.compile("[A-Z]+").toRegex()
  private val lowerCaseCatcher = Pattern.compile("[a-z]+").toRegex()

  override fun convert(source: Any): BigQuerySQLMetadata = convert(source as? Class<*> ?:
  source as? KClass<*> ?: ClassUtils.getUserClass(source))
  fun convert(type: KClass<*>): BigQuerySQLMetadata = convert(type.javaObjectType)
  fun convert(type: Class<*>): BigQuerySQLMetadata {
    if (!AnnotatedElementUtils.hasAnnotation(type, Entity::class.java)) {
      error("The class $type is not an entity")
    }
    val tableName =
      AnnotatedElementUtils.getMergedAnnotation(type, Entity::class.java)?.name
        ?: toSnakeCase(type.simpleName)
    var primaryKey: BigQueryPrimaryKey? = null
    val columns = extractColumns(type) {primaryKey = it }
    return BigQuerySQLMetadata(tableName, primaryKey, columns)
  }


  fun extractId(entity: Any?, clazz: Class<*>? = entity?.let { ClassUtils.getUserClass(it) }): Any? {
    return entity?.let {
      val idField = clazz?.declaredFields?.firstOrNull {
        AnnotatedElementUtils.hasAnnotation(it, Id::class.java)
                || AnnotatedElementUtils.hasAnnotation(it, EmbeddedId::class.java)
      }
      idField?.let {
        it.isAccessible = true
        it.get(it)
      } ?: extractId(entity, clazz?.superclass)
    }
  }
  fun extractColumns(
    type: Class<*>,
    primaryKey: (BigQueryPrimaryKey) -> Unit
  ): Map<String, BigQueryField<*>> {
    if (type == Any::class.java) return emptyMap()
    if (!AnnotatedElementUtils.hasAnnotation(type, MappedSuperclass::class.java)) return extractColumns(
      type.superclass,
      primaryKey
    )
    return type.declaredFields.flatMap { field ->
      field.isAccessible = true
      val columnName =
        AnnotatedElementUtils.getMergedAnnotation(field, Column::class.java)?.name ?: toSnakeCase(field.name)
      val isEmbedded = AnnotatedElementUtils.hasAnnotation(field, EmbeddedId::class.java)
      if (isEmbedded || AnnotatedElementUtils.hasAnnotation(field, Id::class.java)) {
        val embeddedColumns = if(isEmbedded) extractColumns(field.type){} else mapOf(columnName to FieldWrapper<Any>(
          field,
          type
        ))
        primaryKey(
          BigQueryPrimaryKey(
            field, embeddedColumns.entries, isEmbedded
          )
        )
        if (isEmbedded) return@flatMap embeddedColumns.map { Pair(it.key, SubField(FieldWrapper(field, field.declaringClass), it.value)) }
      }
      listOf(Pair(columnName, FieldWrapper<Any>(field, type)))
    }.toMap() + extractColumns(type.superclass, primaryKey)
  }

  private fun toSnakeCase(text: String): String {
    val matcher = upperCaseCatcher.findAll(text)
    val matcher2 = lowerCaseCatcher.findAll(text)

    var result = ""
    val iterator = matcher2.iterator()

    matcher.iterator().forEach {
      val group = it.value
      val start = if (group.length > 1) {
        "_" + group.substring(0..group.length - 2) + "_" + group.last()
      } else "_$group"
      result += start.lowercase() + if (iterator.hasNext()) iterator.next().value else ""
    }
    return result
  }
}