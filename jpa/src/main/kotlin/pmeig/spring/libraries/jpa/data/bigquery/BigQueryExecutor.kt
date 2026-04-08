package pmeig.spring.libraries.jpa.data.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableResult
import org.slf4j.LoggerFactory
import pmeig.spring.libraries.jpa.data.bigquery.converter.entity.model.BigQuerySQLMetadata

private val logger = LoggerFactory.getLogger(BigQueryExecutor::class.java)

class BigQueryExecutor(private val client: BigQuery,
                       private val jobId: JobId.Builder) {

  fun tryAndExecute(query: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder): TableResult? {
    val configuration = configurator(QueryJobConfiguration.newBuilder(query))
    return try {
      client.create(JobInfo.of(configuration.setDryRun(true).build()))
      client.query(configuration.setDryRun(false).build(), jobId.setRandomJob().build())
    } catch (exception: BigQueryException) {
      logger.warn("Execution failed", exception)
      return null
    }
  }

  fun execute(query: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder): TableResult {
    return client.query(configurator(QueryJobConfiguration.newBuilder(query)).build(),
      jobId.setRandomJob().build())
  }

  fun getTable(dataset: String, table: String) = getTable(TableId.of(dataset, table))

  fun getTable(tableId: TableId): Table = client.getTable(tableId)
}