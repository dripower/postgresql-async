package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification
import org.specs2.specification._
import scala.concurrent._
import scala.concurrent.duration.Duration

class PositionalPreparedStatementSpec
    extends Specification
    with DatabaseTestHelper {
  private def run[A](f: PostgreSQLConnection => Future[A]): A = {
    val conn = new PostgreSQLConnection(
      configuration = defaultConfiguration,
      positionalParamHolder = true
    ).connect
      .map(_.asInstanceOf[PostgreSQLConnection])
    try {
      Await.result(conn.flatMap(f), Duration.Inf)
    } finally {
      Await.ready(conn.flatMap(_.disconnect), Duration.Inf)
    }
  }

  val setup = "create temp table foo(i int not null)"

  "conn" should {
    "run postional prepared statement" in {
      val r = run { c =>
        for {
          _ <- c.sendQuery(setup)
          r <- c.sendPreparedStatement(
            "SELECT * FROM foo where i > $1 and i < $2",
            Seq(1, 10)
          )
        } yield r
      }
      r.rows.map(_.size) should ===(Some(0))
    }
  }
}
