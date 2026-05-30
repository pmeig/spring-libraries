package pmeig.spring.libraries.jpa.core.annotation

import org.hibernate.annotations.JdbcType
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS)
@JdbcType(VarcharJdbcType::class)
annotation class Struct()
