package pmeig.spring.libraries.jpa.core.converter.configuration.zoneId

import java.time.ZoneId

@FunctionalInterface
fun interface DataZoneIdProvider {
  fun getZoneId(): ZoneId
}