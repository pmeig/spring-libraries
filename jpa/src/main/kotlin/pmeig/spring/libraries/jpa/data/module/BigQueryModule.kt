package pmeig.spring.libraries.jpa.data.module

import com.google.cloud.bigquery.BigQuery
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.jpa.data.bigquery.BigQueryPage

@Configuration
@ComponentScan(basePackageClasses = [BigQueryPage::class])
@ConditionalOnClass(BigQuery::class)
@ConditionalOnBooleanProperty(prefix = "spring.plugins.pmeig.jpa", name = ["bigquery"], havingValue = true, matchIfMissing = true)
class BigQueryModule {
}