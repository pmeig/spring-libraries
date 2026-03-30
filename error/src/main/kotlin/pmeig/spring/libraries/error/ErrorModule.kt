package pmeig.spring.libraries.error

import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackageClasses = [ErrorModule::class])
class ErrorModule {
}