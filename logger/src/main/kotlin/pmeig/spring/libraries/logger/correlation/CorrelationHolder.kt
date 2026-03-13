package pmeig.spring.libraries.logger.correlation

import java.util.*

private val correlationIdHolder = ThreadLocal<UUID>()

val correlationId: UUID
  get() {
    var id = correlationIdHolder.get()
    if (null == id) {
      id = UUID.randomUUID()
      correlationIdHolder.set(id)
    }
    return id
  }

internal fun insertCorrelationId(id: UUID) = correlationIdHolder.set(id)

internal fun removeCorrelationId() = correlationIdHolder.remove()