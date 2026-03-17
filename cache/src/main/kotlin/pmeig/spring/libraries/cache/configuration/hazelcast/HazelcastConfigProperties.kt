package pmeig.spring.libraries.cache.configuration.hazelcast

import com.hazelcast.config.NetworkConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Properties

@ConfigurationProperties(prefix = "spring.cache.hazelcast")
data class HazelcastConfigProperties(
  var addresses: List<String> = emptyList(),
  var port: Int = NetworkConfig.DEFAULT_PORT,
  var cluster: String? = null,
  var properties: Properties = Properties()
)
