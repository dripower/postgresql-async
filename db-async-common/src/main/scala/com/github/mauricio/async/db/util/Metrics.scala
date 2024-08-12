package com.github.mauricio.async.db.util

import com.google.common.cache._
import java.util.concurrent.atomic._
import java.util.concurrent.TimeUnit
import org.slf4j._
import scala.util._
import scala.concurrent.{Future, ExecutionContext}

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

  private implicit val ec: ExecutionContext = Execution.parasitic

  private def ommitNames(fields: String, max: Int) = {

    @annotation.tailrec
    def go(start: Int, sb: StringBuilder, count: Int): StringBuilder = {
      if (count >= max) {
        if (fields.length > start) {
          sb.append(", ...")
          sb
        } else {
          sb
        }
      } else {
        val sepIndex = fields.indexOf(',', start)
        if (sepIndex != -1) {
          if (count > 0) {
            sb.append(",")
          }
          sb.append(fields.slice(start, sepIndex))
          go(sepIndex + 1, sb, count + 1)
        } else {
          sb.append(fields.slice(start, fields.length))
          sb
        }
      }
    }
    go(0, new StringBuilder, 0).toString
  }

  def stat[T](sql: String, params: Seq[Any])(f: => Future[T]) = {
    val key   = normalize(sql)
    val start = System.currentTimeMillis()
    val fut   = f
    fut.onComplete {
      case _ =>
        val end  = System.currentTimeMillis()
        val time = end - start
        logSlow(key, params, time)
    }
    fut

  }

  private def digestRowNames(sql: String): String = {
    val fromIndex = sql.indexOf("FROM")
    if (sql.startsWith("SELECT ") || sql.startsWith("select ") && fromIndex != -1) {
      val fields = sql.slice(7, fromIndex)
      sql.slice(0, 7) + ommitNames(fields, 2) + " " + sql.slice(fromIndex, sql.length)
    } else {
      sql
    }

  }

  private final val InsertValuesPart = {
    val rowExpr = "\\s*\\([^\\)]+\\)\\s*"
    s"(?im)VALUES(${rowExpr}(\\,${rowExpr})*)".r
  }

  private def digestInsert(sql: String) = {
    InsertValuesPart.replaceAllIn(
      sql,
      { m =>
        "VALUES (...) "
      }
    )
  }

  private def normalize(sql: String) = {
    if (sql.contains("SELECT") || sql.contains("select")) {
      digestRowNames(sql)
    } else if (sql.trim.startsWith("INSERT") || sql.trim.startsWith("insert")) {
      digestInsert(sql)
    } else sql
  }

  private val metricsLogger = LoggerFactory.getLogger("async.sql.log.metrics")
  private val slowLogger    = LoggerFactory.getLogger("async.sql.log.slow")

  private def maxStatStatement = sys.props.get("db.maxStats").map(_.toLong).getOrElse(10000L)

  private def showParam(p: Seq[Any]): String = {
    val sb = new StringBuilder
    sb.append("[")
    p.foreach { x =>
      if (x.isInstanceOf[Seq[Any]]) {
        val xs = x.asInstanceOf[Seq[Any]]
        val xsStr = if (xs.size > 5) {
          xs.take(10).mkString("[", ",", s",...${(xs.size - 5)} more]")
        } else xs.mkString("[", ",", "]")
        sb.append(xsStr)
      } else {
        sb.append(x.toString)
      }
      sb.append(",")
    }
    sb.append("]")
    sb.toString()
  }

  @inline private def logSlow(sql: String, params: Seq[Any], time: Long) = {
    if (time > 50) {
      slowLogger.info(s"SQL:[$sql],TIME:[${time}]ms, params: ${showParam(params)}")
    }
  }
}
