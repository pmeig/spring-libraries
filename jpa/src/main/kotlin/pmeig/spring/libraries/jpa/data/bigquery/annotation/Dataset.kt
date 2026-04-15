package pmeig.spring.libraries.jpa.data.bigquery.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Dataset(val value: String, val project: String = "")
