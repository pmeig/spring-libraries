package pmeig.spring.libraries.jpa.core.entity

import org.junit.jupiter.api.Test
import pmeig.spring.libraries.jpa.core.model.EntityTest

class EntityAnnotationReaderTest {
  val entityAnnotationReader = EntityAnnotationReader()

  @Test
  fun metadataWithClass_success() {
    val metadata = entityAnnotationReader.metadata(EntityTest::class)
    println("metadata: $metadata")
  }
}