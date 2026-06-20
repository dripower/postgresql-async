package com.github.mauricio.async.db.util

import org.specs2.mutable.Specification

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

    "ignore commas inside select expressions" in {
      Metrics.normalize("SELECT concat(first_name, ',', last_name), count(*), created_at FROM users") mustEqual
        "SELECT concat(first_name, ',', last_name), count(*), ... FROM users"
    }

    "collapse insert values" in {
      Metrics.normalize("INSERT INTO users(id, name) VALUES (1, 'a'), (2, 'b')") mustEqual
        "INSERT INTO users(id, name) VALUES (...)"
    }

    "collapse nested insert values and keep the suffix" in {
      Metrics.normalize(
        "insert into events(payload) values (json_build_object('a', 1)), (json_build_object('b', 2)) returning id"
      ) mustEqual
        "insert into events(payload) VALUES (...) returning id"
    }
  }
}
