package pmeig.spring.libraries.jpa.core.auditing

import tools.jackson.core.type.TypeReference
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.FieldAccessorWrapper
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.core.converter.configuration.zoneId.DataZoneIdProvider
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Objects
import java.util.function.Function
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass

private val DATE_TRANSFORM = mapOf<KClass<*>, (ZoneId) -> Any>(
  LocalDateTime::class to { LocalDateTime.now(it) },
  LocalDate::class to { LocalDate.now(it) },
  Date::class to { Date.valueOf(LocalDate.now(it)) },
  Timestamp::class to { Timestamp.valueOf(LocalDateTime.now(it)) },
  ZonedDateTime::class to { ZonedDateTime.now(it) },
  OffsetDateTime::class to { OffsetDateTime.now(it) },
  Time::class to { Time.valueOf(LocalTime.now(it)) },
  LocalTime::class to { LocalTime.now(it) },
  OffsetTime::class to { OffsetTime.now(it) },
  Instant::class to { ZonedDateTime.now(it).toInstant() }
)


@Component
class AuditingManager(
  private val zoneIdProvider: DataZoneIdProvider,
  private val cacheManager: DataCacheManager,
  private val auditorAware: AuditorAware<*>
) {

  fun applyCreatedDate(entity: Any): Boolean = applyAuditingDate(entity, CreatedDate::class)

  fun applyLastModifiedDate(entity: Any): Boolean = applyAuditingDate(entity, LastModifiedDate::class)

  fun applyCreatedBy(entity: Any): Boolean = applyAuditingAuthor(entity, CreatedBy::class)

  fun applyLastModifiedBy(entity: Any): Boolean = applyAuditingAuthor(entity, LastModifiedBy::class)

  @Suppress("UNCHECKED_CAST")
  fun applyAuditing(entity: Any) = (DataCacheNames.useCache(cacheManager, DataCacheNames.AUDITING, "combine::${entity.javaClass.typeName}",
    object: TypeReference<Function<Any, Boolean>>() {}) {
    var combine: Function<Any, Boolean> = { false }
    val createDate = applyCreatedDate(entity)
    if (createDate) {
      combine = {
        applyCreatedBy(it)
      }
    }
    val lastModifiedDate = applyLastModifiedDate(entity)
    if (lastModifiedDate) {
      combine = combine.andThen { applyLastModifiedDate(it) }
    }
    val createdBy = applyCreatedBy(entity)
    if (createdBy) {
      combine = combine.andThen { applyCreatedBy(it) }
    }
    val lastModifiedBy = applyLastModifiedBy(entity)
    if (lastModifiedBy) {
      combine = combine.andThen { applyLastModifiedBy(it) }
    }
    combine
  }).apply(entity)


  private fun applyAuditingAuthor(
    entity: Any,
    annotation: KClass<out Annotation>
  ): Boolean = fromCache(entity, annotation) { fieldAccessor ->
    val author = auditorAware.currentAuditor.map(Objects::toString).getOrNull() ?: return@fromCache { false }
    Function { entityHandle ->
      if (null == fieldAccessor.get(entityHandle)) {
        fieldAccessor.set(entityHandle, author)
      }
      true
    }
  }.apply(entity)

  private fun applyAuditingDate(entity: Any, annotation: KClass<out Annotation>) = fromCache(entity, annotation) { fieldAccessor ->
    val dateTransform = DATE_TRANSFORM[fieldAccessor.java.kotlin] ?: return@fromCache { false }
    Function { entityHandle ->
      if (null == fieldAccessor.get(entityHandle)) {
        fieldAccessor.set(entityHandle, dateTransform(zoneIdProvider.getZoneId()))
      }
      true
    }
  }.apply(entity)

  @Suppress("UNCHECKED_CAST")
  private fun fromCache(entity: Any, annotation: KClass<out Annotation>, compute: (FieldAccessor<Any>) -> Function<Any, Boolean>): Function<Any, Boolean> =
    DataCacheNames.useCache(cacheManager, DataCacheNames.AUDITING, "${annotation.simpleName}::${entity.javaClass.typeName}",
      object: TypeReference<Function<Any, Boolean>>() {}) {
      val fieldAccessor = extractFieldWithAnnotation(entity.javaClass, annotation.java) ?: return@useCache { false } as Function<Any, Boolean>
      compute(fieldAccessor)
    }

  private fun extractFieldWithAnnotation(
    entity: Class<*>,
    annotation: Class<out Annotation>
  ): FieldAccessor<Any>? {
    if (Any::class.java == entity) {
      return null
    }
    val iterator = entity.declaredFields.iterator()
    var field = extractFieldWithAnnotation(entity.superclass, annotation)
    var found = field != null
    while (!found && iterator.hasNext()) {
      field = FieldAccessorWrapper(iterator.next())
      found = field.field.isAnnotationPresent(annotation)
    }
    if (found) {
      return field
    }
    return null
  }
}