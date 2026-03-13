package pmeig.spring.libraries.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.event.Level
import org.springframework.cglib.proxy.InvocationHandler
import org.springframework.cglib.proxy.Proxy
import org.springframework.integration.channel.DirectChannel
import org.springframework.util.ClassUtils
import pmeig.spring.libraries.logger.integration.LoggerMessage
import java.lang.reflect.Method
import java.util.LinkedList
import kotlin.reflect.KClass

internal val logChannel = DirectChannel()

private class PmeigLogger(private val delegate: Logger): InvocationHandler {

  override fun invoke(
    proxy: Any,
    method: Method,
    args: Array<out Any?>
  ): Any? = try {
    val level = Level.valueOf(method.name.uppercase())
    sendLog(level, args)
  } catch (_: Throwable) {
    method.invoke(delegate, *args)
  }

  private fun sendLog(level: Level, args: Array<out Any?>) {
    val allArguments = LinkedList(args.toList())
    var marker: Marker? = null
    var message = allArguments.poll()
    if (message is Marker) {
      marker = message
      message = allArguments.poll()
    }
    send(level, marker, message as? String, allArguments)
  }
  private fun send(level: Level, marker: Marker?, message: String?, args: LinkedList<Any?>) {
    var throwable = args.pollLast()
    if (null != throwable && throwable !is Throwable) {
      args.addLast(throwable)
      throwable = null
    }
    logChannel.send(
      LoggerMessage(delegate.name, message ?: "", level, marker, throwable, *args.toTypedArray())
    )
  }
}

class PmeigLoggerFactory {
   @Suppress("unused")
   companion object {
     @JvmStatic
     fun getLogger(name: String) = getLogger(LoggerFactory.getLogger(name))
     @JvmStatic
     fun getLogger(clazz: KClass<*>) = getLogger(clazz.java)
     @JvmStatic
     fun getLogger(clazz: Class<*>) = getLogger(LoggerFactory.getLogger(clazz))

     private fun getLogger(delegate: Logger): Logger =
       Proxy.newProxyInstance(ClassUtils.getDefaultClassLoader(), arrayOf(Logger::class.java), PmeigLogger(delegate)) as Logger
   }
}