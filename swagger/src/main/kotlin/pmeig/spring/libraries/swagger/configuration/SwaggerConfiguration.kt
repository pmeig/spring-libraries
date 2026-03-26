package pmeig.spring.libraries.swagger.configuration

import io.swagger.v3.oas.annotations.tags.Tag
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.objenesis.ObjenesisStd
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.RestController

@Configuration
class SwaggerConfiguration: BeanPostProcessor {

  private val objenesis = ObjenesisStd()

  override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
    if (AnnotatedElementUtils.hasAnnotation(bean.javaClass, RestController::class.java)
      && !bean.javaClass.packageName.startsWith("org.springdoc")) {
      return super.postProcessAfterInitialization(addTag(bean, beanName), beanName)
    }
    return super.postProcessAfterInitialization(bean, beanName)
  }

  private fun addTag(bean: Any, beanName: String): Any {
    val tag = AnnotatedElementUtils.getMergedAnnotation(bean.javaClass, Tag::class.java)?.name ?: beanName
    if (listOf("Controller", "Endpoint").any { tag.endsWith(it) }) {
      val tagAnnotation = createTagAnnotation(tag)
      val redefined = classRedefined(tagAnnotation, ClassUtils.getUserClass(bean), bean)
      return objenesis.newInstance(redefined)
    }
    return bean
  }
  private fun classRedefined(tagAnnotation: AnnotationDescription, clazz: Class<*>, bean: Any): Class<*> {
    return ByteBuddy().subclass(clazz, ConstructorStrategy.Default.NO_CONSTRUCTORS)
      .annotateType(tagAnnotation)
      .method {
        it.isPublic.and(it.isConstructor.not()).and(it.isFinal.not())
      }.intercept(InvocationHandlerAdapter.of { _, method, args ->
          method.isAccessible = true
          method.invoke(bean, *(args ?: arrayOf()))
      })
      .make()
      .load(
        clazz.classLoader,
        ClassLoadingStrategy.Default.INJECTION
      )
      .loaded
  }

  private fun createTagAnnotation(tag: String) = AnnotationDescription.Builder
    .ofType(Tag::class.java)
    .define("name", tag.removeSuffix("Controller").removeSuffix("Endpoint"))
    .build()
}