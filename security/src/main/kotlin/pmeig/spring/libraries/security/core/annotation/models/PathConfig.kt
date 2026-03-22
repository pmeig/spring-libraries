package pmeig.spring.libraries.security.core.annotation.models

import org.springframework.web.bind.annotation.RequestMethod

data class PathConfig(
  val paths: List<String> = listOf(),
  val method: List<RequestMethod> = listOf(RequestMethod.GET)
)
