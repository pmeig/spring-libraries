package pmeig.spring.libraries.security.core.models.api

import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.MultiValueMap
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.net.URI

class ReactiveAdapter(private val request: ServerHttpRequest? = null): ApiRequest, ApiResponse<Mono<ServerResponse>> {

  private var responseBody: (ServerResponse.BodyBuilder) -> Mono<ServerResponse> = { it.build() }
  private var responseCookie: (ServerResponse.BodyBuilder) -> ServerResponse.BodyBuilder = { it }
  private var responseHeaders: (ServerResponse.BodyBuilder) -> ServerResponse.BodyBuilder = { it }
  private var status = HttpStatus.OK

  override val path: URI
    get() = request?.uri ?: URI.create("http://localhost")
  override val method: HttpMethod
    get() = request?.method ?: HttpMethod.GET

  override fun body(content: Any?): ApiResponse<Mono<ServerResponse>> {
    if (null != content) responseBody = { it.bodyValue(content) }
    return this
  }

  override val body: String?
    get() = request?.body?.toString()

  override fun cookies(supplier: () -> ResponseCookie): ApiResponse<Mono<ServerResponse>> {
    val prev = responseCookie
    responseCookie = { prev(it).cookie(supplier()) }
    return this
  }

  override val cookies: List<HttpCookie>
    get() = request?.cookies?.values?.flatten()?.toList() ?: emptyList()

  override fun headers(consumer: (headers: HttpHeaders) -> Unit): ApiResponse<Mono<ServerResponse>> {
    val prev = responseHeaders
    responseHeaders = { prev(it).headers(consumer) }
    return this
  }

  override val headers: HttpHeaders
    get() = request?.headers ?: HttpHeaders()
  override val params: MultiValueMap<String, String>
    get() = request?.queryParams ?: MultiValueMapAdapter(emptyMap<String, List<String>>())

  override fun status(status: HttpStatus): ApiResponse<Mono<ServerResponse>> {
    this.status = status
    return this
  }

  override fun toResponseEntity(): Mono<ServerResponse>
  = responseBody(responseHeaders(responseCookie(ServerResponse.status(status))))
}