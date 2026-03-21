package pmeig.spring.libraries.logger.correlation.filter

import org.reactivestreams.Subscription
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import pmeig.spring.libraries.logger.PmeigLoggerFactory
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.correlation.insertCorrelationId
import pmeig.spring.libraries.logger.correlation.removeCorrelationId
import reactor.core.CoreSubscriber
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.core.publisher.Operators
import reactor.util.context.Context
import java.util.UUID

@Configuration
@ConditionalOnWebApplication(type = REACTIVE)
@ConditionalOnClass(WebFilter::class)
class ReactiveCorrelationFilter(private val correlationProperties: CorrelationProperties) : WebFilter {
  private val logger = PmeigLoggerFactory.getLogger(ReactiveCorrelationFilter::class.java)

  override fun filter(
    exchange: ServerWebExchange,
    chain: WebFilterChain
  ): Mono<Void> {
    val key = exchange.request.id
    val correlationId = exchange.request.headers.get(correlationProperties.header)?.let {
      val uuid = UUID.fromString(it.first())
      exchange.request.attributes[correlationProperties.request] = uuid
      uuid
    } ?: UUID.randomUUID()
    Hooks.onEachOperator(key, Operators.lift { _, subscriber ->
     if (subscriber is CorrelationContextSubscriber<*>) subscriber as CorrelationContextSubscriber<Any> else
       CorrelationContextSubscriber(subscriber, correlationId)
    })
    logger.info("Request with correlation id {} received", correlationId)
    return chain.filter(
      exchange
    ).contextWrite(Context.of(correlationProperties.request, correlationId))
      .doOnTerminate {
      Hooks.resetOnEachOperator(key)
    }
  }
}

private class CorrelationContextSubscriber<T : Any>(
  private val delegate: CoreSubscriber<T>,
  private val correlation: UUID
) : CoreSubscriber<T> {


  override fun onSubscribe(p0: Subscription) {
    delegate.onSubscribe(p0)
    insertCorrelationId(correlation)

  }

  override fun onNext(p0: T?) = operate { delegate.onNext(p0) }

  override fun onError(p0: Throwable?) = operate { delegate.onError(p0) }

  override fun onComplete() = operate { delegate.onComplete() }

  private fun operate(operation: () -> Unit) {
    insertCorrelationId(correlation)
    operation()
    removeCorrelationId()
  }
}
