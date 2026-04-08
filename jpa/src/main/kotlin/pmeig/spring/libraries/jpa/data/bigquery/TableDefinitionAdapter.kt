package pmeig.spring.libraries.jpa.data.bigquery

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.TableDefinition
import com.google.cloud.bigquery.TableResult
import java.io.Serial

class TableDefinitionAdapter(
  private val delegate: TableResult
): TableDefinition() {

  override fun getType(): Type? = null

  override fun getSchema(): Schema? = delegate.schema

  override fun toBuilder(): Builder<*, *>? = null

  companion object {
    @Serial
    private const val serialVersionUID: Long = 1594339529676253398L
  }
}