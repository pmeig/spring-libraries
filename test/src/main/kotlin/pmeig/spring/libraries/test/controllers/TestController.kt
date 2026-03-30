@file:Suppress("unused")

package pmeig.spring.libraries.test.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pmeig.spring.libraries.cache.core.model.CacheConfig
import pmeig.spring.libraries.cache.core.model.CacheConfigsProperties
import pmeig.spring.libraries.error.web.WebException
import pmeig.spring.libraries.logger.PmeigLoggerFactory
import pmeig.spring.libraries.logger.correlation.correlationId

val logger =
  PmeigLoggerFactory.getLogger(TestController::class)
//  LoggerFactory.getLogger(TestController::class.java)

//@Tag(name = "MyTest")
@RestController
@RequestMapping("test")
class TestController(
  private val configs: List<CacheConfig>,
  private val cacheConfigsProperties: CacheConfigsProperties
) {

  @GetMapping("hello")
//  @Public
  fun hello(): String {
    throw WebException("10", "This is a test exception", HttpStatus.BAD_REQUEST)
    return createMessage("GET with correlation $correlationId")

//    return Mono.delay(Duration.ofSeconds(1)).map {
//      logger.info("Handling {} request {test.log} for hello {} endpoint by {spring.application.name}", "GET",
//        correlationId
//      )
//      createMessage("GET with correlation $correlationId")
//    }
  }

  private fun createMessage(method: String): String {
    return "Hello from $method"
  }

  @PostMapping("hello")
  fun helloPost() = createMessage("POST")

  @PutMapping("hello")
  fun helloPut() = createMessage("PUT")
}