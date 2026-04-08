package pmeig.spring.libraries.jpa.core.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Service
import org.springframework.util.ClassUtils
import pmeig.spring.libraries.jpa.core.accessor.FieldAccessor
import pmeig.spring.libraries.jpa.core.accessor.FieldAccessorWrapper
import pmeig.spring.libraries.jpa.core.accessor.ParentFieldAccessor
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.core.entity.model.DataPrimaryMetadata
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.BigQueryField
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.FieldWrapper
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.SubField
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQueryPrimaryKey
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQuerySQLMetadata
import java.util.regex.Pattern
import kotlin.reflect.KClass

@Service
class EntityAnnotationReader {
  private val upperCaseCatcher = Pattern.compile("[A-Z]+").toRegex()
  private val lowerCaseCatcher = Pattern.compile("[a-z]+").toRegex()

  private val cache = mutableMapOf<Class<*>, DataMetadata>()

  fun metadata(source: Any): DataMetadata = metadata(
    source as? Class<*> ?: source as? KClass<*> ?: ClassUtils.getUserClass(source)
  )

  fun metadata(type: KClass<*>): DataMetadata = metadata(type.javaObjectType)
  fun metadata(type: Class<*>): DataMetadata {
    if (!AnnotatedElementUtils.hasAnnotation(type, Entity::class.java)) {
      error("The class $type is not an entity")
    }
    return cache.computeIfAbsent(type) {
      val tableName =
        AnnotatedElementUtils.getMergedAnnotation(type, Entity::class.java)?.name
          ?: toSnakeCase(type.simpleName)
      val (columns, primaryKey) = extractColumns(type)
      DataMetadata(tableName, primaryKey ?: error("No primary key found for entity $type"), columns)
    }
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
    type: Class<*>
  ): Pair<Map<String, FieldAccessor<*>>, DataPrimaryMetadata?> {
    if (type == Any::class.java) return Pair(emptyMap(), DataPrimaryMetadata())
    if (!AnnotatedElementUtils.hasAnnotation(type, MappedSuperclass::class.java)) return extractColumns(
      type.superclass
    )
    val parentMetadata = extractColumns(type.superclass)
    var dataPrimaryMetadata: DataPrimaryMetadata? = null
    return type.declaredFields.flatMap { field ->
      val columnName =
        AnnotatedElementUtils.getMergedAnnotation(field, Column::class.java)?.name ?: toSnakeCase(field.name)
      val isEmbedded = AnnotatedElementUtils.hasAnnotation(field, EmbeddedId::class.java)
      if (isEmbedded || AnnotatedElementUtils.hasAnnotation(field, Id::class.java)) {
        val embeddedColumns = if (isEmbedded) extractColumns(field.type).first else mapOf(
          columnName to FieldAccessorWrapper<Any>(field)
        )
        dataPrimaryMetadata = DataPrimaryMetadata(
          field, embeddedColumns, isEmbedded
        )
        if (isEmbedded) return@flatMap embeddedColumns.map {
          Pair(
            it.key,
            ParentFieldAccessor(field, it.value)
          )
        }
      }
      listOf(Pair(columnName, FieldAccessorWrapper<Any>(field)))
    }.let {
      Pair(it.toMap() + parentMetadata.first, dataPrimaryMetadata ?: parentMetadata.second)
    }
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