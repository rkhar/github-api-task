package scalac.domain.http

case class CacheSettings(initialCapacity: Int, maxCapacity: Int, timeToLive: Int, timeToIdle: Int)
