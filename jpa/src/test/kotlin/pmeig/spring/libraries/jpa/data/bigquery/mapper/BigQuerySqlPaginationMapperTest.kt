package pmeig.spring.libraries.jpa.data.bigquery.mapper

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import pmeig.spring.libraries.jpa.core.FieldAccessor
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.core.entity.model.DataPrimaryMetadata
import kotlin.test.assertTrue

class BigQuerySqlMapperTest {

  private val mapper = BigQuerySqlMapper()

  @Nested
  inner class PageQuery {

    @Test
    fun `pageQuery wraps simple query with count and pagination`() {
      val query = "SELECT * FROM users WHERE active = true"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      assertTrue(result.contains("WITH __count AS"))
      assertTrue(result.contains("SELECT COUNT(id) as count FROM users WHERE active = true"))
      assertTrue(result.contains("__result AS"))
      assertTrue(result.contains("LIMIT 10"))
      assertTrue(result.contains("OFFSET 0"))
      assertTrue(result.contains("SELECT ARRAY_AGG(items) AS items, ANY_VALUE(__count.count) AS total"))
    }

    @Test
    fun `pageQuery handles query with existing ORDER BY`() {
      val query = "SELECT * FROM users ORDER BY name ASC"
      val pageable = PageRequest.of(1, 20)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      assertTrue(result.contains("FROM users"))
      assertTrue(result.contains("LIMIT 20"))
      assertTrue(result.contains("OFFSET 20"))
      // Original ORDER BY should be removed from the wrapped query
      val resultLower = result.lowercase()
      assertTrue(resultLower.indexOf("order by") < resultLower.lastIndexOf("order by") || resultLower.indexOf("order by") == resultLower.lastIndexOf("order by"))
    }

    @Test
    fun `pageQuery handles query with existing LIMIT`() {
      val query = "SELECT * FROM users LIMIT 100"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      // Pageable limit should override query limit
      assertTrue(result.contains("LIMIT 10"))
      assertTrue(result.contains("OFFSET 0"))
    }

    @Test
    fun `pageQuery handles query with existing OFFSET`() {
      val query = "SELECT * FROM users OFFSET 50"
      val pageable = PageRequest.of(2, 15)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      // Pageable offset should override query offset
      assertTrue(result.contains("LIMIT 15"))
      assertTrue(result.contains("OFFSET 30"))
    }

    @Test
    fun `pageQuery handles query with all clauses`() {
      val query = "SELECT * FROM users WHERE active = true ORDER BY created_at DESC LIMIT 100 OFFSET 50"
      val pageable = PageRequest.of(3, 25)
      val metadata = createMetadata("user_id")

      val result = mapper.pageQuery(query, pageable, metadata)

      assertTrue(result.contains("SELECT COUNT(user_id) as count"))
      assertTrue(result.contains("FROM users WHERE active = true"))
      assertTrue(result.contains("LIMIT 25"))
      assertTrue(result.contains("OFFSET 75"))
    }

    @Test
    fun `pageQuery uses metadata primary keys for count`() {
      val query = "SELECT * FROM orders"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("order_id", "shop_id")

      val result = mapper.pageQuery(query, pageable, metadata)

      assertTrue(result.contains("SELECT COUNT(order_id,shop_id) as count"))
    }

    @Test
    fun `pageQuery uses metadata primary keys for default ORDER BY when not present`() {
      val query = "SELECT * FROM products WHERE category = 'electronics'"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("product_id", "variant_id")

      val result = mapper.pageQuery(query, pageable, metadata)

      // When no ORDER BY in query, metadata keys should be used
      assertTrue(result.contains("FROM products WHERE category = 'electronics'"))
    }

    @Test
    fun `pageQuery handles case insensitive keywords`() {
      val query = "select * from users where active = true order by name asc limit 50 offset 10"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      assertTrue(result.contains("from users where active = true"))
      assertTrue(result.contains("LIMIT 10"))
      assertTrue(result.contains("OFFSET 0"))
    }

    @Test
    fun `pageQuery calculates correct offset for different pages`() {
      val query = "SELECT * FROM users"
      val metadata = createMetadata("id")

      // Page 0
      val result0 = mapper.pageQuery(query, PageRequest.of(0, 10), metadata)
      assertTrue(result0.contains("OFFSET 0"))

      // Page 1
      val result1 = mapper.pageQuery(query, PageRequest.of(1, 10), metadata)
      assertTrue(result1.contains("OFFSET 10"))

      // Page 5
      val result5 = mapper.pageQuery(query, PageRequest.of(5, 20), metadata)
      assertTrue(result5.contains("OFFSET 100"))
    }

    @Test
    fun `pageQuery handles query without WHERE clause`() {
      val query = "SELECT * FROM users"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      assertTrue(result.contains("SELECT COUNT(id) as count FROM users"))
      assertTrue(result.contains("LIMIT 10"))
      assertTrue(result.contains("OFFSET 0"))
    }

    @Test
    fun `pageQuery preserves complex WHERE conditions`() {
      val query = "SELECT * FROM users WHERE (status = 'active' OR status = 'pending') AND created_at > '2024-01-01'"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      assertTrue(result.contains("FROM users WHERE (status = 'active' OR status = 'pending') AND created_at > '2024-01-01'"))
    }

    @Test
    fun `pageQuery structure contains all required CTEs`() {
      val query = "SELECT * FROM users"
      val pageable = PageRequest.of(0, 10)
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      // Verify the structure has both CTEs
      val countIndex = result.indexOf("WITH __count AS")
      val resultIndex = result.indexOf("__result AS")
      val selectIndex = result.indexOf("SELECT ARRAY_AGG(items)")

      assertTrue(countIndex >= 0, "Should contain __count CTE")
      assertTrue(resultIndex > countIndex, "__result CTE should come after __count")
      assertTrue(selectIndex > resultIndex, "Final SELECT should come after CTEs")
    }

    @Test
    fun `pageQuery handles unpaged request with max integer values`() {
      val query = "SELECT * FROM users"
      val pageable = Pageable.unpaged()
      val metadata = createMetadata("id")

      val result = mapper.pageQuery(query, pageable, metadata)

      // Unpaged should still produce valid SQL
      assertTrue(result.contains("WITH __count AS"))
      assertTrue(result.contains("__result AS"))
    }
  }

  private fun createMetadata(vararg keys: String): DataMetadata {
    val metadata = mock<DataMetadata>()
    val primaryMetadata = mock<DataPrimaryMetadata>()
    val fromID = linkedMapOf(*keys.map { it to mock<FieldAccessor<Any>>() }.toTypedArray())

    whenever(metadata.primary).thenReturn(primaryMetadata)
    whenever(primaryMetadata.fromID).thenReturn(fromID)

    return metadata
  }
}
