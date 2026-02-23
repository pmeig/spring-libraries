package pmeig.spring.libraries.security.core.annotation.services

import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pmeig.spring.libraries.security.core.annotation.PmeigSecurity
import pmeig.spring.libraries.security.core.annotation.models.GlobalSecurity
import pmeig.spring.libraries.security.core.annotation.models.PathConfig
import pmeig.spring.libraries.security.core.annotation.models.PmeigAuthorization
import java.lang.reflect.Method
import java.util.function.Consumer

@Service
class SecurityAnnotationService(applicationContext: ApplicationContext) {

  private val scanner =
    ClassPathScanningCandidateComponentProvider(false)
  private val basePackage =
    applicationContext.getBeansWithAnnotation<SpringBootApplication>().values.first().javaClass.`package`.name

  init {
    scanner.addIncludeFilter { reader, _ ->
      reader.annotationMetadata.isAnnotated(RestController::class.qualifiedName!!) &&
              (reader.annotationMetadata.isAnnotated(PmeigSecurity::class.qualifiedName!!) ||
                      reader.annotationMetadata.hasAnnotatedMethods(PmeigSecurity::class.qualifiedName!!))
    }
  }

  fun findAllSecurityAnnotations(): Collection<PmeigAuthorization> {
    val authorizations = mutableListOf<PmeigAuthorization>()
    scanner.findCandidateComponents(basePackage)
      .forEach {
        val classname = Class.forName(it.beanClassName)
        val parentPath = extractPaths(classname)
        val globals = AnnotatedElementUtils
          .getAllMergedAnnotations(classname, PmeigSecurity::class.java)
        val apply = createInjectorGlobalSecurity(globals)
        authorizations.addAll(
          toPmeigAuthorization(
            parentPath,
            classname.declaredMethods.filter(filterMethods(globals)),
            apply
          )
        )
      }
    return authorizations
  }

  private fun toPmeigAuthorization(
    parentPath: List<String>,
    methods: List<Method>,
    globalInjector: Consumer<PmeigAuthorization>
  ) = methods.map { method ->
    createAuthorization(parentPath, method, globalInjector)
  }

  private fun createAuthorization(
    parentPath: List<String>,
    method: Method,
    globalInjector: Consumer<PmeigAuthorization>
  ): PmeigAuthorization {
    val config = extractConfig(method, parentPath)
    val securities = AnnotatedElementUtils.findAllMergedAnnotations(method, PmeigSecurity::class.java).ifEmpty {
      setOf(PmeigSecurity())
    }
    val auth = PmeigAuthorization(config.paths.toTypedArray(), config.method)
    globalInjector.accept(auth)
    securities.takeWhile {
      auth.public = auth.public || it.public
      auth.denied = auth.denied || it.public
      !auth.public && !auth.denied
    }.forEach {
      val setterFeatures = if (it.accepted) PmeigAuthorization::addAccepted else PmeigAuthorization::addRejected
      setterFeatures(auth, it.value, it.type)
    }
    return auth

  }

  private fun filterMethods(globals: MutableSet<PmeigSecurity>): (Method) -> Boolean =
    if (globals.isNotEmpty()) {
      {
        AnnotatedElementUtils.hasAnnotation(
          it,
          RequestMapping::class.java
        )
      }
    } else {
      {
        AnnotatedElementUtils.hasAnnotation(it, PmeigSecurity::class.java)
      }
    }

  private fun createInjectorGlobalSecurity(
    globals: Set<PmeigSecurity>
  ): Consumer<PmeigAuthorization> {

    var isPublic = false
    var isDenied = false
    val globalSecurity = GlobalSecurity()
    globals.filter { security ->
      isPublic = isPublic || security.public
      isDenied = isDenied || security.denied
      !isPublic && !isDenied
    }.forEach { security ->
      globalSecurity.addFeature(security.accepted, mapFeatures(security), security.type)
    }
    val apply = Consumer<PmeigAuthorization> { auth ->
      auth.apply {
        public = public || isPublic
        denied = denied || isDenied
      }
    }
    return if (isPublic || isDenied) apply else {
      val globalConsumer = if (globalSecurity.accepted.isNotEmpty()) apply.andThen { authorization ->
        authorization.accepted.addAll(globalSecurity.accepted)
      } else apply
      if (globalSecurity.denied.isNotEmpty()) globalConsumer.andThen { authorization ->
        authorization.rejected.addAll(globalSecurity.denied)
      } else globalConsumer
    }
  }


  private fun mapFeatures(security: PmeigSecurity): List<String> {
    return security.value.map { feature -> "${security.prefix}$feature" }
  }

  private fun extractPaths(clazz: Class<*>) = (AnnotatedElementUtils
    .getMergedAnnotation(clazz, RequestMapping::class.java)?.path?.toList() ?: listOf(""))
    .map { "/$it" }

  private fun extractConfig(method: Method, prefix: List<String>): PathConfig {
    val requestMapping = AnnotatedElementUtils
      .getMergedAnnotation(method, RequestMapping::class.java) ?: RequestMapping()
    val path = requestMapping.path.toList()
    if (path.isEmpty()) return PathConfig(path, requestMapping.method.toList())
    return PathConfig(prefix.ifEmpty { listOf("") }.flatMap {
      val prefixPath = if (it.isEmpty()) "" else "$it/"
      path.map { next -> "$prefixPath$next" }
    }, requestMapping.method.toList())
  }
}