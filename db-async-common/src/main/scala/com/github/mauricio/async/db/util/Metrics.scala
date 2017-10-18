package com.github.mauricio.async.db.util

import com.google.common.cache._
import Execution.Implicits.trampoline
import java.util.concurrent.atomic._
import java.util.concurrent.TimeUnit
import org.slf4j._
import scala.util._
import scala.concurrent.Future

case class Stat(
  min: AtomicLong = new AtomicLong(Long.MaxValue),
  max: AtomicLong = new AtomicLong(0),
  total: AtomicLong = new AtomicLong(0),
  times: AtomicLong = new AtomicLong(0)) {

  def add(duration: Long) = {
    if(duration < min.get()) {
      min.set(duration)
    }
    if(duration > max.get()) {
      max.set(duration)
    }
    times.incrementAndGet()
    total.addAndGet(duration)
  }
}

object Metrics {

  private val metricsLogger = LoggerFactory.getLogger("async.metrics")
  private val slowLogger = LoggerFactory.getLogger("async.slow")

  val stats = CacheBuilder
    .newBuilder()
    .maximumSize(100000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build(new CacheLoader[String, Stat] {
      def load(key: String) = {
        Stat()
      }
    }
  )

  def stat[T](key: String)(f: => Future[T]) = {
    val start = System.currentTimeMillis()
    val fut = f
    fut.onSuccess {
      case _ =>
        val end = System.currentTimeMillis()
        val time = end - start
        stats.get(key).add(time)
        logSlow(key, time)
        logMetrics(key)
    }
    fut
  }

  @inline private def logSlow(sql: String, time: Long) = {
    if(time > 50) {
      slowLogger.info(s"SQL:[$sql],TIME:[${time}]ms")
    }
  }

  @inline private def logMetrics(key: String) = {
    val stat = stats.get(key)
    val t = stat.total.get()
    val c = stat.times.get()
    val min = stat.min.get()
    val max = stat.max.get()
    if(c % 1000 == 0) {
      metricsLogger.info(s"[SQL-$key], count:$c, avg:${t/math.max(1, c)}ms, max:${max}, min:${min}")
    }
  }
}
