package pmeig.spring.libraries.logger.integration.configurer.log.mdc

import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.support.ResourcePatternResolver
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.util.Properties


@Configuration
class SpringPropertiesMDCConfigurer(
  private val env: Environment,
  private val resourceLoader: ResourcePatternResolver,
  private val yamlParser: Yaml
) : MDCConfigurer {
  private var properties: Map<String, String>? = null

  override fun configure(): Map<String, String> {
    if (null == properties) {
      val propertiesName = extractYamlProperties().toMutableSet()
      propertiesName.addAll(extractProperties())
      properties = propertiesName.associateWith { env.getProperty(it, "null") }
    }
    return properties!!
  }

  private fun extractProperties() =
    extractKeys("properties") { Properties().apply { load(it) }.stringPropertyNames().toList() }

  private fun extractYamlProperties() = extractKeys("y*ml") { extractKeyFromMap(yamlParser.load(it)) }

  private fun extractKeys(extension: String, method: (InputStream) -> List<String>): MutableSet<String> {
    val keys = resourceLoader.getResources("classpath*:application.$extension").flatMap {
      method(it.inputStream)
    }.toMutableSet()
    env.activeProfiles.forEach { profile ->
      keys.addAll(
        resourceLoader.getResources("classpath*:application-$profile.$extension")
          .flatMap { method(it.inputStream) })
    }
    return keys
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractKeyFromMap(map: Map<String, Any>, prefix: String = ""): List<String> =
    map.entries.flatMap { (key, value) ->
      val parent = "$prefix$key"
      if (value is Map<*, *>) {
        extractKeyFromMap(value as Map<String, Any>, "$parent.")
      } else listOf(parent)
    }.toList()
}