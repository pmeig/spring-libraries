package pmeig.spring.libraries.cache

import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackageClasses = [CacheModule::class])
class CacheModule {}