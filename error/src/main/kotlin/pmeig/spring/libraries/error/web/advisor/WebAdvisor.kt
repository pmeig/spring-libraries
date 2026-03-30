package pmeig.spring.libraries.error.web.advisor

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.function.ServerResponse

@ControllerAdvice
@ConditionalOnWebApplication(type = SERVLET)
class ServletAdvisor: WebAdvisorAdapter<ResponseEntity<Any>>({it.toResponse()}) {
}

@ControllerAdvice
@ConditionalOnWebApplication(type = REACTIVE)
class ReactiveAdvisor: WebAdvisorAdapter<ServerResponse>({it.toReactiveResponse()}) {
}