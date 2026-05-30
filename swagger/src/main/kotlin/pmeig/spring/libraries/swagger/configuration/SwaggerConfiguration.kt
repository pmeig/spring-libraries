package pmeig.spring.libraries.swagger.configuration

import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfiguration: OpenApiCustomizer {

  override fun customise(openApi: OpenAPI) {
    openApi.paths!!.forEach { (_, pathItem) ->
      pathItem.readOperations().forEach { operation ->
        operation.tags = operation.tags.map {
          it.replace("-controller", "").replace("-endpoint", "")
        }
      }
    }
  }
}