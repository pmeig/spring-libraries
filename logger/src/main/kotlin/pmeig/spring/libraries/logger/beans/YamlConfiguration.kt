package pmeig.spring.libraries.logger.beans

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml

@ConditionalOnMissingBean(Yaml::class)
@Configuration
class YamlConfiguration {

  @Bean
  @ConditionalOnMissingBean(DumperOptions::class)
  fun dumperOptions() = DumperOptions()

  @Bean
  @ConditionalOnMissingBean(LoaderOptions::class)
  fun loaderOptions() = LoaderOptions()

  @Bean
  fun yaml(loadingConfig: LoaderOptions, dumperOptions: DumperOptions) = Yaml(loadingConfig, dumperOptions)
}