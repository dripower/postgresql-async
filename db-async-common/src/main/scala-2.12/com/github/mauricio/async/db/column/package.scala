package com.github.mauricio.async.db

/**
 * Polyfill for String.toIntOption in Scala 2.12
 */
package object column {
  implicit class RichString(val s: String) extends AnyVal {
    def toIntOption: Option[Int] = {
      try {
        Some(s.toInt)
      } catch {
        case _: NumberFormatException => None
      }
    }
  }
}
