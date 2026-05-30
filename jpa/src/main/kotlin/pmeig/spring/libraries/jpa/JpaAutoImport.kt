package pmeig.spring.libraries.jpa

import org.springframework.context.annotation.ComponentScan
import pmeig.spring.libraries.jpa.core.CoreModule
import pmeig.spring.libraries.jpa.data.module.BigQueryModule

@ComponentScan(basePackageClasses = [CoreModule::class, BigQueryModule::class])
class JpaAutoImport

