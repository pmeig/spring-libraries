package pmeig.spring.libraries.jpa.core.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Service
import org.springframework.util.ClassUtils
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.FieldAccessorWrapper
import pmeig.spring.libraries.jpa.core.ParentFieldAccessor
import pmeig.spring.libraries.jpa.core.annotation.Struct
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.core.entity.model.DataPrimaryMetadata
import java.lang.reflect.Field
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
  @Suppress("UNCHECKED_CAST")
  fun metadata(type: Class<*>): DataMetadata {
    if (!AnnotatedElementUtils.hasAnnotation(type, Entity::class.java)) {
      error("The class $type is not an entity")
    }
    return cache.computeIfAbsent(type) {
      val tableName =
        AnnotatedElementUtils.getMergedAnnotation(type, Entity::class.java)?.name?.ifEmpty {
          toSnakeCase(type.simpleName)
        }
          ?: toSnakeCase(type.simpleName)
      val (columns, primaryKey) = extractColumns(type)
      val constructor = type.declaredConstructors.find { it.parameterCount == 0 } ?: error("No default constructor found for entity $type")
      DataMetadata(tableName, primaryKey ?: error("No primary key found for entity $type"),
        columns as Map<String, FieldAccessor<Any>>
      ) {
        constructor.newInstance()
      }
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

  @Suppress("UNCHECKED_CAST")
  fun extractColumns(
    type: Class<*>
  ): Pair<Map<String, FieldAccessor<*>>, DataPrimaryMetadata?> {
    if (type == Any::class.java) return Pair(emptyMap(), DataPrimaryMetadata())
    if (listOf(MappedSuperclass::class.java, Entity::class.java).none { AnnotatedElementUtils.hasAnnotation(type, it) }) return extractColumns(
      type.superclass
    )
    val parentMetadata = extractColumns(type.superclass)
    var dataPrimaryMetadata: DataPrimaryMetadata? = null
    return type.declaredFields.flatMap { field ->
      val columnName = extractColumnName(field)
      var struct: Map<String, FieldAccessor<*>> = emptyMap()
      val isEmbedded = AnnotatedElementUtils.hasAnnotation(field, EmbeddedId::class.java)
      if (isEmbedded || AnnotatedElementUtils.hasAnnotation(field, Id::class.java)) {
        val (embeddedColumns, partDataPrimaryMetadata) = extractPartId(field, columnName, isEmbedded)
        dataPrimaryMetadata = partDataPrimaryMetadata ?: dataPrimaryMetadata
        if (embeddedColumns.isNotEmpty()) return@flatMap embeddedColumns
      } else if(AnnotatedElementUtils.hasAnnotation(field, Struct::class.java)) {
        struct = extractStructAccessors(field).mapValues {
          ParentFieldAccessor(field, it.value as FieldAccessor<Any>)
        }
      }
      listOf(Pair(columnName, FieldAccessorWrapper(field, struct)))
    }.let {
      Pair(it.toMap() + parentMetadata.first, dataPrimaryMetadata ?: parentMetadata.second)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractStructAccessors(parent: Field): Map<String, FieldAccessor<*>> {
    return parent.type.declaredFields.associate {
      val name = extractColumnName(it)
      var struct = emptyMap<String, FieldAccessor<*>>()
      if (AnnotatedElementUtils.hasAnnotation(it, Struct::class.java)) {
        struct = extractStructAccessors(it).mapValues {
          entry -> ParentFieldAccessor(it, entry.value as FieldAccessor<Any>)
        }
      }
      name to FieldAccessorWrapper<Any>(it, struct)
    }

  }

  private fun extractColumnName(field: Field) = AnnotatedElementUtils
    .getMergedAnnotation(field, Column::class.java)?.name?.ifEmpty {
    toSnakeCase(field.name)
  } ?: toSnakeCase(field.name)

  @Suppress("UNCHECKED_CAST")
  private fun extractPartId(
    field: Field,
    columnName: String,
    isEmbedded: Boolean
  ): Pair<List<Pair<String, FieldAccessorWrapper<Any>>>, DataPrimaryMetadata?> {
      val embeddedColumns = if (isEmbedded) extractColumns(field.type).first else mapOf(
        columnName to FieldAccessorWrapper<Any>(field)
      )
      val dataPrimaryMetadata = DataPrimaryMetadata(
        field, embeddedColumns as Map<String, FieldAccessor<Any>>, isEmbedded
      )
      if (isEmbedded) return Pair(embeddedColumns.map {
        Pair(
          it.key,
          ParentFieldAccessor(field, it.value)
        )
      }, dataPrimaryMetadata)
    return Pair(listOf(), dataPrimaryMetadata)
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
    return if(result.isNotEmpty()) result.substring(1) else text
  }
}