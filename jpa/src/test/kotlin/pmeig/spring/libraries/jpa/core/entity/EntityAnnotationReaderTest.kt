package pmeig.spring.libraries.jpa.core.entity

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.cache.DataCacheNames
import pmeig.spring.libraries.jpa.shared.expected.entityTestMetadata_expected
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import kotlin.test.expect

class EntityAnnotationReaderTest {
  val cacheManager: DataCacheManager = mock()
  val entityAnnotationReader = EntityAnnotationReader(cacheManager)

  @Test
  fun metadataWithKClass_success() {
    whenever(cacheManager.getCache(DataCacheNames.METADATA)).thenReturn(null)

    expect(entityTestMetadata_expected()) {
      entityAnnotationReader.metadata(EntityTest::class).apply {
        println(columns["struct"]?.struct)
        entityTestMetadata_expected().columns.forEach { (name, accessor) ->
          println(name + ": " + (accessor == columns[name]))
        }
      }
    }
  }
}