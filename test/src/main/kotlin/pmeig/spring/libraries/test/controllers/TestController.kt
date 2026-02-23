package pmeig.spring.libraries.test.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("test")
class TestController(
) {
  @GetMapping("hello")
  fun hello() = "Hello from GET"

  private fun createMessage(method: String): String {
    return "Hello from $method"
  }

  @PostMapping("hello")
  fun helloPost() = createMessage("POST")

  @PutMapping("hello")
  fun helloPut() = createMessage("PUT")
}