package pmeig.spring.libraries.jpa.core.cache

import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.set
import org.springframework.cglib.proxy.Proxy
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class LocalCacheManager : ConcurrentMapCacheManager() {
  private val executor = Executors.newSingleThreadScheduledExecutor(
    Thread.ofVirtual()
      .name("local-cache-cleaner", 0)
      .factory()
  )

  override fun getCache(name: String): Cache {
    val cache = super.getCache(name) ?: super.createConcurrentMapCache(name)
    return Proxy.newProxyInstance(cache.javaClass.classLoader, arrayOf(Cache::class.java)) { proxy, method, args ->
      if ("set" == method.name) {
        val key = args[0]
        val schedulesCache = super.getCache("__dataSchedules")!!
        schedulesCache[key] = createCleaner(key, cache, schedulesCache)
        cache[key] = args[1]
      } else if ("get" == method.name) {
        val key = args[0]
        val schedulesCache = super.getCache("__dataSchedules")!!
        schedulesCache.get(key, ScheduledFuture::class.java)!!.cancel(false)
        schedulesCache[key] = createCleaner(key, cache, schedulesCache)
        cache[key]
      } else method.invoke(cache, args)
    } as Cache
  }

  private fun createCleaner(key: Any, cache: Cache, schedulesCache: Cache) =
    executor.schedule({
      cache.evictIfPresent(key)
      schedulesCache.evictIfPresent(key)
    }, 4, TimeUnit.HOURS)
}