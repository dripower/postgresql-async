package com.github.mauricio.async.db.util

import org.specs2.mutable.Specification
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class MetricsSpec extends Specification {

  "Metrics.normalize" should {

    "omit select fields after the first two" in {
      Metrics.normalize("SELECT id, name, created_at FROM users") mustEqual
        "SELECT id, name, ... FROM users"
    }

    "find select keywords ignoring case and leading whitespace" in {
      Metrics.normalize("  select id, name, created_at from users") mustEqual
        "  select id, name, ... from users"
    }

    "find from keyword after line breaks" in {
      Metrics.normalize("SELECT id,\nname,\ncreated_at\nFROM users") mustEqual
        "SELECT id,\nname, ... FROM users"
    }

    "finish select expressions with commas inside functions" in {
      Metrics.normalize("SELECT concat(first_name, ',', last_name), count(*), created_at FROM users") mustEqual
        "SELECT concat(first_name, ', ... FROM users"
    }

    "skip quoted strings at the end of select statements" in {
      Metrics.normalize("SELECT 6578, 'this is some text'") mustEqual
        "SELECT 6578, 'this is some text'"
    }

    "skip quoted identifiers at the end of select statements" in {
      Metrics.normalize("SELECT 6578, `this is some text`") mustEqual
        "SELECT 6578, `this is some text`"
    }

    "skip line comments at the end of select statements" in {
      Metrics.normalize("SELECT 6578 -- trailing comment") mustEqual
        "SELECT 6578 -- trailing comment"
    }

    "skip block comments at the end of select statements" in {
      Metrics.normalize("SELECT 6578 /* trailing comment */") mustEqual
        "SELECT 6578 /* trailing comment */"
    }

    "handle unclosed quoted strings" in {
      Metrics.normalize("SELECT 6578, 'this is some text") mustEqual
        "SELECT 6578, 'this is some text"
    }

    "handle unclosed block comments" in {
      Metrics.normalize("SELECT 6578 /* trailing comment") mustEqual
        "SELECT 6578 /* trailing comment"
    }

    "collapse insert values" in {
      Metrics.normalize("INSERT INTO users(id, name) VALUES (1, 'a'), (2, 'b')") mustEqual
        "INSERT INTO users(id, name) VALUES (...)"
    }

    "collapse insert values followed by parentheses" in {
      Metrics.normalize("INSERT INTO users(id, name) VALUES(1, 'a'), (2, 'b')") mustEqual
        "INSERT INTO users(id, name) VALUES (...)"
    }

    "collapse nested insert values" in {
      Metrics.normalize(
        "insert into events(payload) values (json_build_object('a', 1)), (json_build_object('b', 2)) returning id"
      ) mustEqual
        "insert into events(payload) VALUES (...)"
    }

    "finish for generated SQL fragments with quotes comments and delimiters" in {
      val random    = new Random(1L)
      val fragments = IndexedSeq(
        "id",
        "name",
        ",",
        "(",
        ")",
        "'text'",
        "'unterminated",
        "\"identifier\"",
        "`identifier`",
        "-- comment",
        "/* block */",
        "/* unterminated",
        "FROM table_name",
        "WHERE id = ?",
        "VALUES (?, ?)"
      )

      foreach(1 to 1000) { _ =>
        val sql =
          (if (random.nextBoolean()) "SELECT " else "INSERT INTO t ") +
            (1 to random.nextInt(20)).map(_ => fragments(random.nextInt(fragments.length))).mkString(" ")
        Await.result(Future(Metrics.normalize(sql)), 100.millis) must not(throwA[Throwable])
      }
    }
  }
}
