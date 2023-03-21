package com.github.mauricio.async.db.util

import org.specs2.mutable.Specification
import scala.concurrent.Future

class MetricsSpec extends Specification {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  "Metrics" should {

    "normalize select" in {
      val sql = "SELECT x.f1, x.f2, x.f3, x.f4 FROM foo"
      Metrics
        .stat(sql) {
          Future(Thread.sleep(100))
        }
      Thread.sleep(110)
      println(Metrics.stats.asMap())
      Metrics.stats.getIfPresent("SELECT x.f1, x.f2, ... FROM foo") !== null
    }

    "normalize update" in {
      val sql = "INSERT INTO foo(id) VALUES (1), (2), (3), \n(4) RETURNING id;"
      Metrics.stat(sql) {
        Future(Thread.sleep(100))
      }
      Thread.sleep(102)
      Metrics.stats.getIfPresent("INSERT INTO foo(id) VALUES (...) RETURNING id;") !== null
    }

  }

}
