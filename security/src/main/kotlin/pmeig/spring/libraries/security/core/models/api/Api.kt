package pmeig.spring.libraries.security.core.models.api

import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.util.MultiValueMap
import java.net.URI

interface ApiRequest {
  val path: URI
  val method: HttpMethod
  val body: String?
  val cookies: List<HttpCookie>
  val headers: HttpHeaders
  val params: MultiValueMap<String, String>
}

interface ApiResponse<T> {
  fun status(status: HttpStatus): ApiResponse<T>
  fun body(content: Any?): ApiResponse<T>
  fun headers(consumer: (headers: HttpHeaders) -> Unit): ApiResponse<T>
  fun cookies(supplier: () -> ResponseCookie): ApiResponse<T>
  fun toResponseEntity(): T
}
