package com.github.mauricio.async.db.util

import com.google.common.cache._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic._
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

  private def omitNames(fields: String, max: Int) = {

    val sb        = new StringBuilder
    var start     = 0
    var count     = 0
    var depth     = 0
    var i         = 0
    var truncated = false
    while (i < fields.length && !truncated) {
      val next = nextSqlTokenIndex(fields, i)
      if (next > i) {
        i = next
      } else {
        fields.charAt(i) match {
          case '(' =>
            depth += 1
          case ')' if depth > 0 =>
            depth -= 1
          case ',' if depth == 0 && count + 1 >= max && hasNonWhitespace(fields, i + 1) =>
            appendField(sb, fields, start, i, count)
            sb.append(", ...")
            truncated = true
          case ',' if depth == 0 =>
            appendField(sb, fields, start, i, count)
            count += 1
            start = i + 1
          case _ =>
        }
        i += 1
      }
    }

    if (!truncated) {
      appendField(sb, fields, start, fields.length, count)
    }
    sb.toString
  }

  def stat[T](sql: String, params: Seq[Any], start: Long)(future: Future[T]) = {
    val key = normalize(sql)
    future.onComplete {
      case _ =>
        val time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        logMetrics(key, time)
        logSlow(key, params, time)
    }
    future

  }

  private def digestRowNames(sql: String, selectIndex: Int): String = {
    val fieldsStart = skipWhitespace(sql, selectIndex + SelectKeyword.length)
    val fromIndex   = findTopLevelKeyword(sql, FromKeyword, fieldsStart)
    if (fromIndex != -1) {
      val fields = sql.slice(fieldsStart, fromIndex).trim
      sql.slice(0, fieldsStart) + omitNames(fields, 2) + " " + sql.slice(fromIndex, sql.length)
    } else {
      sql
    }

  }

  private def digestInsert(sql: String) = {
    val insertIndex = firstNonWhitespace(sql)
    val valuesIndex = findTopLevelKeyword(sql, ValuesKeyword, insertIndex + InsertKeyword.length)
    if (valuesIndex == -1) {
      sql
    } else {
      var rowStart = skipWhitespace(sql, valuesIndex + ValuesKeyword.length)
      var rowEnd   = -1
      var continue = rowStart < sql.length && sql.charAt(rowStart) == '('
      var valid    = true
      while (continue && valid) {
        val end = findClosingParen(sql, rowStart)
        if (end == -1) {
          valid = false
        } else {
          rowEnd = end + 1
          val next = skipWhitespace(sql, rowEnd)
          if (next < sql.length && sql.charAt(next) == ',') {
            rowStart = skipWhitespace(sql, next + 1)
            continue = rowStart < sql.length && sql.charAt(rowStart) == '('
          } else {
            continue = false
          }
        }
      }
      if (!valid || rowEnd == -1) {
        sql
      } else {
        val suffix     = sql.slice(rowEnd, sql.length).trim
        val normalized = sql.slice(0, valuesIndex) + "VALUES (...)"
        if (suffix.isEmpty) {
          normalized
        } else {
          s"$normalized $suffix"
        }
      }
    }
  }

  private[util] def normalize(sql: String) = {
    val start = firstNonWhitespace(sql)
    if (startsWithKeyword(sql, start, SelectKeyword)) {
      digestRowNames(sql, start)
    } else if (startsWithKeyword(sql, start, InsertKeyword)) {
      digestInsert(sql)
    } else sql
  }

  private final val SelectKeyword = "select"
  private final val FromKeyword   = "from"
  private final val InsertKeyword = "insert"
  private final val ValuesKeyword = "values"

  private def appendField(sb: StringBuilder, fields: String, start: Int, end: Int, count: Int): Unit = {
    if (count > 0) {
      sb.append(",")
    }
    sb.append(fields.slice(start, end))
  }

  @annotation.tailrec
  private def firstNonWhitespace(sql: String, index: Int = 0): Int = {
    if (index >= sql.length || !sql.charAt(index).isWhitespace) {
      index
    } else {
      firstNonWhitespace(sql, index + 1)
    }
  }

  @annotation.tailrec
  private def skipWhitespace(sql: String, index: Int): Int = {
    if (index >= sql.length || !sql.charAt(index).isWhitespace) {
      index
    } else {
      skipWhitespace(sql, index + 1)
    }
  }

  private def findTopLevelKeyword(sql: String, keyword: String, from: Int): Int = {
    var i      = from
    var depth  = 0
    var result = -1
    while (i < sql.length && result == -1) {
      val next = nextSqlTokenIndex(sql, i)
      if (next > i) {
        i = next
      } else {
        sql.charAt(i) match {
          case '(' =>
            depth += 1
          case ')' if depth > 0 =>
            depth -= 1
          case _ if depth == 0 && startsWithKeyword(sql, i, keyword) =>
            result = i
          case _ =>
        }
        i += 1
      }
    }
    result
  }

  private def findClosingParen(sql: String, openIndex: Int): Int = {
    var i      = openIndex
    var depth  = 0
    var result = -1
    while (i < sql.length && result == -1) {
      val next = nextSqlTokenIndex(sql, i)
      if (next > i) {
        i = next
      } else {
        sql.charAt(i) match {
          case '(' =>
            depth += 1
          case ')' =>
            depth -= 1
            if (depth == 0) {
              result = i
            }
          case _ =>
        }
        i += 1
      }
    }
    result
  }

  private def startsWithKeyword(sql: String, index: Int, keyword: String): Boolean = {
    index >= 0 &&
    index + keyword.length <= sql.length &&
    sql.regionMatches(true, index, keyword, 0, keyword.length) &&
    (index == 0 || !isIdentifierPart(sql.charAt(index - 1))) &&
    (index + keyword.length == sql.length || !isIdentifierPart(sql.charAt(index + keyword.length)))
  }

  private def isIdentifierPart(c: Char): Boolean = {
    c.isLetterOrDigit || c == '_' || c == '$'
  }

  private def nextSqlTokenIndex(sql: String, index: Int): Int = {
    sql.charAt(index) match {
      case '\'' | '"' | '`' =>
        skipQuoted(sql, index)
      case '-' if index + 1 < sql.length && sql.charAt(index + 1) == '-' =>
        skipLineComment(sql, index + 2)
      case '/' if index + 1 < sql.length && sql.charAt(index + 1) == '*' =>
        skipBlockComment(sql, index + 2)
      case _ =>
        index
    }
  }

  private def skipQuoted(sql: String, start: Int): Int = {
    val quote  = sql.charAt(start)
    var i      = start + 1
    var result = -1
    while (i < sql.length && result == -1) {
      if (sql.charAt(i) == '\\') {
        i += 2
      } else if (sql.charAt(i) == quote) {
        if (i + 1 < sql.length && sql.charAt(i + 1) == quote) {
          i += 2
        } else {
          result = i + 1
        }
      } else {
        i += 1
      }
    }
    if (result == -1) sql.length else result
  }

  private def skipLineComment(sql: String, start: Int): Int = {
    var i = start
    while (i < sql.length && sql.charAt(i) != '\n' && sql.charAt(i) != '\r') {
      i += 1
    }
    i
  }

  private def skipBlockComment(sql: String, start: Int): Int = {
    var i      = start
    var result = -1
    while (i + 1 < sql.length && result == -1) {
      if (sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') {
        result = i + 2
      }
      i += 1
    }
    if (result == -1) sql.length else result
  }

  private def hasNonWhitespace(sql: String, start: Int): Boolean = {
    var i      = start
    var result = false
    while (i < sql.length && !result) {
      if (!sql.charAt(i).isWhitespace) {
        result = true
      }
      i += 1
    }
    result
  }

  private val metricsLogger = LoggerFactory.getLogger("async.sql.log.metrics")
  private val slowLogger    = LoggerFactory.getLogger("async.sql.log.slow")

  private def maxStatStatement = sys.props.get("db.maxStats").map(_.toLong).getOrElse(10000L)

  private def showParam(p: Seq[Any]): String = {
    val sb = new StringBuilder
    sb.append("[")
    p.foreach { x =>
      if (x.isInstanceOf[Seq[Any]]) {
        val xs    = x.asInstanceOf[Seq[Any]]
        val xsStr = if (xs.size > 5) {
          xs.take(10).mkString("[", ",", s",...${(xs.size - 5)} more]")
        } else xs.mkString("[", ",", "]")
        sb.append(xsStr)
      } else {
        val xAsStr = if (x == null) "null" else x.toString()
        sb.append(xAsStr)
      }
      sb.append(",")
    }
    sb.append("]")
    sb.toString()
  }

  @inline private def logMetrics(sql: String, time: Long) = {
    if (metricsLogger.isInfoEnabled) {
      metricsLogger.info("SQL:[{}],TIME:[{}]ms", logArgs(sql, time): _*)
    }
  }

  @inline private def logSlow(sql: String, params: Seq[Any], time: Long) = {
    if (time > 50 && slowLogger.isInfoEnabled) {
      slowLogger.info(
        "SQL:[{}],TIME:[{}]ms, params: {}",
        logArgs(sql, time, showParam(params)): _*
      )
    }
  }

  private def logArgs(sql: String, time: Long, args: AnyRef*) = {
    Array[AnyRef](sql, time: java.lang.Long) ++ args
  }
}
