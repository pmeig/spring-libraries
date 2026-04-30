package pmeig.spring.libraries.jpa.core.cache

import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("ttl-cleaner", 0).factory())

class TTLCache(
  private val delegate: Cache,
  private val ttlDuration: Duration = Duration.ofHours(4)
) : Cache {
  private val ttl = ConcurrentMapCache("DATA_TTL")

  override fun getName() = delegate.name
  override fun getNativeCache() = delegate.nativeCache
  override fun get(key: Any) = getValue(key) { delegate.get(key) }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> get(key: Any, type: Class<T>?) = getValue(key) { delegate.get(key, type) }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> get(key: Any, valueLoader: Callable<T>) = getValue(key) { delegate.get(key, valueLoader) }

  override fun put(key: Any, value: Any?) {
    createTTL(key)
    delegate.put(key, value)
  }

  override fun evict(key: Any) {
    removeTTL(key)
    delegate.evict(key)
  }

  override fun clear() {
    ttl.nativeCache.values.forEach {
      schedule -> (schedule as ScheduledFuture<*>).cancel(true)
    }
    ttl.clear()
  }

  private fun <T : Any> getValue(key: Any, retrieve: () -> T?): T? {
    removeTTL(key)
    createTTL(key)
    return retrieve()
  }

  private fun removeTTL(key: Any) {
    ttl.get(key, ScheduledFuture::class.java)?.let {
      it.cancel(false)
      ttl.evict(key)
    }
  }

  private fun createTTL(key: Any) {
    ttl.put(key, executor.schedule({
      ttl.evict(key)
      delegate.evict(key)
    }, ttlDuration.toMillis(), TimeUnit.MILLISECONDS))
  }
}