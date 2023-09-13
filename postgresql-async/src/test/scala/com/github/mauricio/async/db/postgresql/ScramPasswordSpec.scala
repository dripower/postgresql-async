package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.Configuration
import org.specs2.mutable.Specification

class ScramPasswordSpec extends Specification with DatabaseTestHelper {
  "handler" should {
    "connect to database with scram password" in {
      val cfg = Configuration(
        port = 5432,
        username = "postgres_scram",
        password = Some("postgres_scram"),
        database = databaseName
      )
      withHandler(
        cfg,
        { (conn) =>
          val result = executeQuery(conn, "SELECT 2")
          val row    = result.rows.get(0)
          println(row)
          row(0) === (2)
        }
      )
    }
  }
}
