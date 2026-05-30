package pmeig.spring.libraries.jpa.core.entity.model

data class DataMetadata(
  val table: String,
  val primary: DataPrimaryMetadata,
  val reference: Class<*>,
  val columns: DataColumns = DataColumns(),
  val createEntity: () -> Any
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataMetadata

    if (table != other.table) return false
    if (primary != other.primary) return false
    if (columns != other.columns) return false

    return true
  }

  override fun hashCode(): Int {
    var result = table.hashCode()
    result = 31 * result + primary.hashCode()
    result = 31 * result + columns.hashCode()
    return result
  }
}
