@file:Suppress("unused")

package pmeig.spring.libraries.swagger.annotation

import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Operation
@RequestMapping
annotation class RestMapping(
  @get:AliasFor(annotation = RequestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RequestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RequestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RequestMapping::class)
  val method: Array<RequestMethod> = [],
  @get:AliasFor(annotation = RequestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RequestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RequestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RequestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RequestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = Operation::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = Operation::class)
  val summary: String = "",
  @get:AliasFor(annotation = Operation::class)
  val description: String = "",
  @get:AliasFor(annotation = Operation::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = Operation::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = Operation::class)
  val operationId: String = "",
  @get:AliasFor(annotation = Operation::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = Operation::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = Operation::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = Operation::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = Operation::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = Operation::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = Operation::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = Operation::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.GET])
annotation class Get(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.POST])
annotation class Post(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.PUT])
annotation class Put(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.DELETE])
annotation class Delete(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.PATCH])
annotation class Patch(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.HEAD])
annotation class Head(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.OPTIONS])
annotation class Options(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RestMapping(method = [RequestMethod.TRACE])
annotation class Trace(
  @get:AliasFor(annotation = RestMapping::class)
  val name: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val value: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val path: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val params: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val headers: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val produces: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val consumes: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val version: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val tags: Array<String> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val summary: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val description: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val requestBody: RequestBody = RequestBody(),
  @get:AliasFor(annotation = RestMapping::class)
  val externalDocs: ExternalDocumentation = ExternalDocumentation(),
  @get:AliasFor(annotation = RestMapping::class)
  val operationId: String = "",
  @get:AliasFor(annotation = RestMapping::class)
  val parameters: Array<Parameter> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val responses: Array<ApiResponse> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val deprecated: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val security: Array<SecurityRequirement> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val servers: Array<Server> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val extensions: Array<Extension> = [],
  @get:AliasFor(annotation = RestMapping::class)
  val hidden: Boolean = false,
  @get:AliasFor(annotation = RestMapping::class)
  val ignoreJsonView: Boolean = false
)



