package pmeig.spring.libraries.jpa.data.bigquery

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration
import org.springframework.stereotype.Component

@Component
class Test(private val bigquery: BigQuery) {
  fun test() {
    bigquery.query(QueryJobConfiguration.of("SELECT * FROM `pmeig-spring-libraries.test.test` LIMIT 10"))
  }
}