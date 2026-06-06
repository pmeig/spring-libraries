package pmeig.spring.libraries.security.core.model.api

import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.http.server.ServerHttpRequest
import org.springframework.util.MultiValueMap
import org.springframework.util.MultiValueMapAdapter
import java.net.URI

class ServletAdapter(
  private val request: ServerHttpRequest? = null
) : ApiRequest, ApiResponse<ResponseEntity<*>> {
  private var responseBody: (ResponseEntity.BodyBuilder) -> ResponseEntity<*> = { it.build<Unit>() }
  private var responseHeaders: (ResponseEntity.BodyBuilder) -> ResponseEntity.BodyBuilder = { it }
  private var status = HttpStatus.OK
  override val path: URI = request?.uri ?: URI.create("http://localhost")
  override val method: HttpMethod = request?.method ?: HttpMethod.GET
  override fun body(content: Any?): ApiResponse<ResponseEntity<*>> {
    responseBody = if (null != content) {
      { it.body(content) }
    } else {
      { it.build<Unit>() }
    }
    return this
  }

  override val body: String?
    get() = request?.body?.readAllBytes()?.toString(Charsets.UTF_8)

  override fun cookies(supplier: () -> ResponseCookie): ApiResponse<ResponseEntity<*>> =
    headers { it.add(HttpHeaders.SET_COOKIE, supplier().toString()) }

  override val cookies: List<HttpCookie>
    get() = request?.headers?.get("Cookie")?.flatMap {
      it.split(";").map { value ->
        value.trim()
      }.filter { value -> value.contains("=") }
        .map { value ->
          val cookie = value.split("=", limit = 2)
          HttpCookie(cookie[0], cookie[1])
        }
    } ?: emptyList()

  override fun headers(consumer: (headers: HttpHeaders) -> Unit): ApiResponse<ResponseEntity<*>> {
    val prev = responseHeaders
    responseHeaders = { prev(it).headers(consumer) }
    return this
  }

  override val headers: HttpHeaders
    get() = request?.headers ?: HttpHeaders()

  override val params: MultiValueMap<String, String>
    get() = MultiValueMapAdapter(path.rawQuery.split("&").stream().map {
      val value = it.split("=", limit = 2)
      Pair(value[0], value[1])
    }.reduce(mutableMapOf<String, MutableList<String>>(), {acc, pair ->
      acc.getOrPut(pair.first) { mutableListOf() }.add(pair.second)
      acc
    }, {first, second -> first.apply { putAll(second) }}))

  override fun status(status: HttpStatus): ApiResponse<ResponseEntity<*>> {
    this.status = status
    return this
  }

  override fun toResponseEntity(): ResponseEntity<*> {
    return responseBody(responseHeaders(ResponseEntity.status(status)))
  }
}