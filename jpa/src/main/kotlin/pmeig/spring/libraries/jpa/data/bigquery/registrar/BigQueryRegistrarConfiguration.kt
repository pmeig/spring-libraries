package pmeig.spring.libraries.jpa.data.bigquery.registrar

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportSelector
import org.springframework.core.type.AnnotationMetadata

@Configuration
@Import(BigQueryRegistrarConfiguration.BigQueryImport::class)
class BigQueryRegistrarConfiguration {

  internal class BigQueryImport: ImportSelector {
    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<out String> {
      return arrayOf(BigQueryRegistrar::class.java.typeName)
    }
  }
}