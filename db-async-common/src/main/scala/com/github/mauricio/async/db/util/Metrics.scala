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
  times: AtomicLong = new AtomicLong(0)
) {

  def add(duration: Long) = {
    if (duration < min.get()) {
      min.set(duration)
    }
    if (duration > max.get()) {
      max.set(duration)
    }
    times.incrementAndGet()
    total.addAndGet(duration)
  }
}

object Metrics {

  private val metricsLogger = LoggerFactory.getLogger("async.metrics")
  private val slowLogger    = LoggerFactory.getLogger("async.slow")

  val stats = CacheBuilder
    .newBuilder()
    .maximumSize(100000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build(new CacheLoader[String, Stat] {
      def load(key: String) = {
        Stat()
      }
    })

  def stat[T](key: String)(f: => Future[T]) = {
    val start = System.currentTimeMillis()
    val fut   = f
    fut.onSuccess {
      case _ =>
        val end  = System.currentTimeMillis()
        val time = end - start
        logSlow(key, time)
    }
    fut
  }

  @inline private def logSlow(sql: String, time: Long) = {
    if (time > 100) {
      slowLogger.info(s"SQL:[${shortKey(sql)},TIME:[${time}]ms")
    }
  }

  @inline def shortKey(k: String) = {
    if (k.length() > 100 && k.startsWith("SELECT ")) {
      val fromIdx = k.indexOf("FROM")
      if (fromIdx != -1) {
        val fields     = k.substring("SELECT ".length(), fromIdx)
        val firstComma = fields.indexOf(',')
        if (firstComma != -1) {
          val firstField = fields.substring(0, firstComma)
          s"SELECT ${firstField},... ${k.substring(fromIdx)}"
        } else {
          k
        }
      } else k
    } else k
  }
}
