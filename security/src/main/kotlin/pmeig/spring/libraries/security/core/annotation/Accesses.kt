@file:Suppress("unused")

package pmeig.spring.libraries.security.core.annotation

import org.springframework.core.annotation.AliasFor


@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PmeigSecurity(
  vararg val value: String = [],
  val public: Boolean = false,
  val denied: Boolean = false,
  val accepted: Boolean = true,
  val prefix: String = "",
  val type: Type = Type.OR
) {

  enum class Type { AND, OR }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PmeigSecurity(public = true)
annotation class Public

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PmeigSecurity(denied = true)
annotation class Denied



@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PmeigSecurity(prefix = "ROLE_")
annotation class Roles(
  @get:AliasFor(annotation = PmeigSecurity::class)
  vararg val value: String = [],
  @get:AliasFor(annotation = PmeigSecurity::class)
  val type: PmeigSecurity.Type = PmeigSecurity.Type.OR
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PmeigSecurity
annotation class Features(
  @get:AliasFor(annotation = PmeigSecurity::class)
  vararg val value: String = [],
  @get:AliasFor(annotation = PmeigSecurity::class)
  val type: PmeigSecurity.Type = PmeigSecurity.Type.OR
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PmeigSecurity(prefix = "ROLE_", accepted = false)
annotation class NoRoles(
  @get:AliasFor(annotation = PmeigSecurity::class)
  vararg val value: String = [],
  @get:AliasFor(annotation = PmeigSecurity::class)
  val type: PmeigSecurity.Type = PmeigSecurity.Type.OR
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PmeigSecurity(accepted = false)
annotation class NoFeatures(
  @get:AliasFor(annotation = PmeigSecurity::class)
  vararg val value: String = [],
  @get:AliasFor(annotation = PmeigSecurity::class)
  val type: PmeigSecurity.Type = PmeigSecurity.Type.OR
)

