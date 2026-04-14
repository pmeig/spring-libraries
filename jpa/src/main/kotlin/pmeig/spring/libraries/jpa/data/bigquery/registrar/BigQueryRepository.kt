package pmeig.spring.libraries.jpa.data.bigquery.registrar

import org.springframework.stereotype.Component

@Component
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class BigQueryRepository()
