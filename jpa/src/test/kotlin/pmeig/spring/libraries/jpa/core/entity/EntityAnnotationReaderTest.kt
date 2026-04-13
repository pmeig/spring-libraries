package pmeig.spring.libraries.jpa.core.entity

import org.junit.jupiter.api.Test
import pmeig.spring.libraries.jpa.shared.expected.entityTestMetadata_expected
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import kotlin.test.expect

class EntityAnnotationReaderTest {
  val entityAnnotationReader = EntityAnnotationReader()

  @Test
  fun metadataWithKClass_success() {
    println(entityTestMetadata_expected().columns["struct"]?.struct)
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