package pmeig.spring.libraries.jpa.core.entity

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.stereotype.Service
import org.springframework.util.ClassUtils
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.FieldAccessorWrapper
import pmeig.spring.libraries.jpa.core.ParentFieldAccessor
import pmeig.spring.libraries.jpa.core.annotation.Struct
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.entity.model.DataColumns
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.core.entity.model.DataPrimaryMetadata
import java.lang.reflect.Field
import java.util.regex.Pattern
import kotlin.reflect.KClass

@Service
class EntityAnnotationReader(
  private val cacheManager: DataCacheManager
) {
  private val upperCaseCatcher = Pattern.compile("[A-Z]+").toRegex()
  private val lowerCaseCatcher = Pattern.compile("[a-z]+").toRegex()

  fun metadata(source: Any): DataMetadata = metadata(
    source as? Class<*> ?: source as? KClass<*> ?: ClassUtils.getUserClass(source)
  )

  fun metadata(type: KClass<*>): DataMetadata = metadata(type.javaObjectType)

  @Suppress("UNCHECKED_CAST")
  fun metadata(type: Class<*>) = DataCacheNames.useCache(
    cacheManager, DataCacheNames.METADATA,
    type.typeName, DataMetadata::class
  ) {
    if (!AnnotatedElementUtils.hasAnnotation(type, Entity::class.java)) {
      error("The class $type is not an entity")
    }
    val tableAnnotation = AnnotatedElementUtils.getMergedAnnotation(type, Table::class.java)
    val fragmentsTableName = listOfNotNull(tableAnnotation?.catalog?.ifEmpty { null },
      tableAnnotation?.schema?.ifEmpty { null }, tableAnnotation?.name?.ifEmpty { null }
        ?: AnnotatedElementUtils.getMergedAnnotation(type, Entity::class.java)?.name?.ifEmpty {
          null
        } ?: toSnakeCase(type.simpleName))
    val tableName = fragmentsTableName.joinToString(".")
    val (columns, primaryKey) = extractColumns(type) as Pair<Map<String, FieldAccessor<Any>>, DataPrimaryMetadata?>
    val constructor = type.declaredConstructors.find { it.parameterCount == 0 }
      ?: error("No default constructor found for entity $type")
    DataMetadata(
      "`$tableName`", primaryKey ?: error("No primary key found for entity $type"),
      type,
      DataColumns(columns, columns
        .filter {
          !AnnotatedElementUtils.hasAnnotation((it.value as FieldAccessorWrapper<Any>).field, CreatedDate::class.java)
                  && !AnnotatedElementUtils.hasAnnotation((it.value as FieldAccessorWrapper<Any>).field, CreatedBy::class.java)
        })
    ) {
      constructor.newInstance()
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun extractColumns(
    type: Class<*>
  ): Pair<Map<String, FieldAccessor<*>>, DataPrimaryMetadata?> =
    DataCacheNames.useCache(cacheManager, DataCacheNames.COLUMNS, type.typeName, Pair::class) {
      if (type == Any::class.java) return@useCache Pair(emptyMap<String, FieldAccessor<*>>(), DataPrimaryMetadata())
      if (listOf(MappedSuperclass::class.java, Entity::class.java).none {
          AnnotatedElementUtils.hasAnnotation(
            type,
            it
          )
        }) return@useCache extractColumns(
        type.superclass
      )
      val parentMetadata = extractColumns(type.superclass)
      var dataPrimaryMetadata: DataPrimaryMetadata? = null
      type.declaredFields.flatMap { field ->
        val columnName = extractColumnName(field)
        var struct: Map<String, FieldAccessor<*>> = emptyMap()
        val isEmbedded = AnnotatedElementUtils.hasAnnotation(field, EmbeddedId::class.java)
        if (isEmbedded || AnnotatedElementUtils.hasAnnotation(field, Id::class.java)) {
          val (embeddedColumns, partDataPrimaryMetadata) = extractPartId(field, columnName, isEmbedded)
          dataPrimaryMetadata = partDataPrimaryMetadata ?: dataPrimaryMetadata
          if (embeddedColumns.isNotEmpty()) return@flatMap embeddedColumns
        } else if (AnnotatedElementUtils.hasAnnotation(field, Struct::class.java)) {
          struct = extractStructAccessors(field).mapValues {
            ParentFieldAccessor(field, it.value as FieldAccessor<Any>)
          }
        }
        listOf(Pair(columnName, FieldAccessorWrapper(field, struct)))
      }.let {
        Pair(it.toMap() + parentMetadata.first, dataPrimaryMetadata ?: parentMetadata.second)
      }
    } as Pair<Map<String, FieldAccessor<*>>, DataPrimaryMetadata?>

  @Suppress("UNCHECKED_CAST")
  private fun extractStructAccessors(parent: Field): Map<String, FieldAccessor<*>> =
    parent.type.declaredFields.associate {
      val name = extractColumnName(it)
      var struct = emptyMap<String, FieldAccessor<*>>()
      if (AnnotatedElementUtils.hasAnnotation(it, Struct::class.java)) {
        struct = extractStructAccessors(it).mapValues { entry ->
          ParentFieldAccessor(it, entry.value as FieldAccessor<Any>)
        }
      }
      name to FieldAccessorWrapper<Any>(it, struct)
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
    var embeddedColumns = if (isEmbedded) extractColumns(field.type).first else mapOf(
      columnName to FieldAccessorWrapper<Any>(field)
    )
    val fromEntityColumns = if (isEmbedded) embeddedColumns.map { Pair(it.key, ParentFieldAccessor(field, it.value)) }
            as List<Pair<String, FieldAccessorWrapper<Any>>>
    else {
      val fromEntity = embeddedColumns.toList() as List<Pair<String, FieldAccessorWrapper<Any>>>
      embeddedColumns = emptyMap()
      fromEntity
    }
    val dataPrimaryMetadata = DataPrimaryMetadata(
      FieldAccessorWrapper(field),
      fromEntityColumns.toMap() as Map<String, FieldAccessor<Any>>,
      embeddedColumns as Map<String, FieldAccessor<Any>>,
      isEmbedded
    )
    return Pair(fromEntityColumns, dataPrimaryMetadata)
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
    return if (result.isNotEmpty()) result.substring(1) else text
  }
}