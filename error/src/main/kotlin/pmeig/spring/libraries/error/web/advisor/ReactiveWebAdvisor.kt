package pmeig.spring.libraries.error.web.advisor

import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.MethodNotAllowedException
import org.springframework.web.server.NotAcceptableStatusException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.UnsupportedMediaTypeStatusException
import org.springframework.web.servlet.function.ServerResponse
import pmeig.spring.libraries.logger.PmeigLoggerFactory
import reactor.core.publisher.Mono
import java.net.URI

abstract class ReactiveWebAdvisor: WebAdvisorAdapter<ServerResponse>({it.toReactiveResponse()}) {
  private val log = PmeigLoggerFactory.getLogger(javaClass)

  /**
   * Gère les erreurs de validation (@Valid sur les @RequestBody, etc.)
   * et retourne un ProblemDetail avec les erreurs de champs.
   */
  @ExceptionHandler(WebExchangeBindException::class)
  protected open fun handleWebExchangeBindException(
    ex: WebExchangeBindException,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.warn("Validation error: {}", ex.message)

    val fieldErrors = ex.bindingResult.fieldErrors.associate {
      it.field to (it.defaultMessage ?: "Invalid value")
    }

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.BAD_REQUEST,
      "Request validation failed with ${fieldErrors.size} error(s)"
    ).apply {
      title = "Validation Failed"
      type = URI.create("https://problems.http.dev/validation-error")
      instance = URI.create(exchange.request.path.value())
      setProperty("errors", fieldErrors)
    }

    return buildProblemResponse(problem)
  }

  /**
   * Gère les erreurs d'input (paramètres manquants, types invalides).
   */
  @ExceptionHandler(ServerWebInputException::class)
  protected open fun handleServerWebInputException(
    ex: ServerWebInputException,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.warn("Input error: {}", ex.message)

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.BAD_REQUEST,
      ex.reason ?: "Invalid request input"
    ).apply {
      title = "Bad Request"
      type = URI.create("https://problems.http.dev/bad-request")
      instance = URI.create(exchange.request.path.value())
    }

    return buildProblemResponse(problem)
  }

  /**
   * Gère les erreurs de décodage (JSON malformé, etc.).
   */
  @ExceptionHandler(DecodingException::class)
  protected open fun handleDecodingException(
    ex: DecodingException,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.warn("Decoding error: {}", ex.message)

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.BAD_REQUEST,
      "Unable to decode request body"
    ).apply {
      title = "Malformed Request"
      type = URI.create("https://problems.http.dev/malformed-request")
      instance = URI.create(exchange.request.path.value())
    }

    return buildProblemResponse(problem)
  }

  /**
   * Gère les erreurs 405 Method Not Allowed.
   */
  @ExceptionHandler(MethodNotAllowedException::class)
  protected open fun handleMethodNotAllowedException(
    ex: MethodNotAllowedException,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.warn("Method not allowed: {}", ex.message)

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.METHOD_NOT_ALLOWED,
      ex.reason ?: "HTTP method not supported for this endpoint"
    ).apply {
      title = "Method Not Allowed"
      type = URI.create("https://problems.http.dev/method-not-allowed")
      instance = URI.create(exchange.request.path.value())
      setProperty("allowedMethods", ex.supportedMethods.map { m -> m.name() })
    }

    return buildProblemResponse(problem).flatMap {
      ex.supportedMethods.let { methods ->
        Mono.just(ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
          .headers { it.allow = methods.toSet() }
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(problem))
      }
    }
  }

  /**
   * Gère les erreurs 415 Unsupported Media Type.
   */
  @ExceptionHandler(UnsupportedMediaTypeStatusException::class)
  protected open fun handleUnsupportedMediaTypeException(
    ex: UnsupportedMediaTypeStatusException,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.warn("Unsupported media type: {}", ex.message)

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.UNSUPPORTED_MEDIA_TYPE,
      ex.reason ?: "Content-Type not supported"
    ).apply {
      title = "Unsupported Media Type"
      type = URI.create("https://problems.http.dev/unsupported-media-type")
      instance = URI.create(exchange.request.path.value())
    }

    return buildProblemResponse(problem)
  }

  /**
   * Gère les erreurs 406 Not Acceptable.
   */
  @ExceptionHandler(NotAcceptableStatusException::class)
  protected open fun handleNotAcceptableException(
    ex: NotAcceptableStatusException,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.warn("Not acceptable: {}", ex.message)

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.NOT_ACCEPTABLE,
      ex.reason ?: "Requested media type not acceptable"
    ).apply {
      title = "Not Acceptable"
      type = URI.create("https://problems.http.dev/not-acceptable")
      instance = URI.create(exchange.request.path.value())
    }

    return buildProblemResponse(problem)
  }

  /**
   * Gère les ResponseStatusException (exceptions avec status HTTP explicite).
   */
  @ExceptionHandler(ResponseStatusException::class)
  protected open fun handleResponseStatusException(
    ex: ResponseStatusException,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.warn("Response status exception ({}): {}", ex.statusCode, ex.reason)

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.valueOf(ex.statusCode.value()),
      ex.reason ?: ex.statusCode.toString()
    ).apply {
      title = ex.statusCode.toString()
      instance = URI.create(exchange.request.path.value())
    }

    return buildProblemResponse(problem)
  }

  /**
   * Handler générique pour les exceptions non gérées.
   */
  @ExceptionHandler(Exception::class)
  protected open fun handleGenericException(
    ex: Exception,
    exchange: ServerWebExchange
  ): Mono<ServerResponse> {
    log.error("Unhandled exception: {}", ex.message, ex)

    val problem = ProblemDetail.forStatusAndDetail(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "An unexpected error occurred"
    ).apply {
      title = "Internal Server Error"
      instance = URI.create(exchange.request.path.value())
    }

    return buildProblemResponse(problem)
  }

  /**
   * Construit une réponse ServerResponse à partir d'un ProblemDetail.
   * Peut être surchargée pour personnaliser les headers.
   */
  protected open fun buildProblemResponse(problem: ProblemDetail): Mono<ServerResponse> {
    return Mono.just(ServerResponse.status(problem.status)
      .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
      .body(problem))
  }
}