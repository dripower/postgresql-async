package com.github.mauricio.async.db.column

import java.time.LocalDate
import org.specs2.mutable.Specification

class DateEncoderDecoderSpec extends Specification {

  val encoderDecoder = DateEncoderDecoder

  "DateEncoderDecoder" should {

    "decode standard date format" in {
      val date = encoderDecoder.decode("2023-12-25")
      date.getYear mustEqual 2023
      date.getMonthValue mustEqual 12
      date.getDayOfMonth mustEqual 25
    }

    "decode date with leading zeros" in {
      val date = encoderDecoder.decode("2023-01-05")
      date.getYear mustEqual 2023
      date.getMonthValue mustEqual 1
      date.getDayOfMonth mustEqual 5
    }

    "decode zeroed date as null" in {
      encoderDecoder.decode("0000-00-00") must beNull
    }

    "encode LocalDate" in {
      val date = LocalDate.of(2023, 12, 25)
      encoderDecoder.encode(date) mustEqual "2023-12-25"
    }

    "encode java.sql.Date" in {
      val sqlDate = new java.sql.Date(123, 11, 25) // Note: java.sql.Date year is 1900-based
      encoderDecoder.encode(sqlDate) mustEqual "2023-12-25"
    }

    "encode date with leading zeros" in {
      val date = LocalDate.of(2023, 1, 5)
      encoderDecoder.encode(date) mustEqual "2023-01-05"
    }

    "handle malformed date with exception" in {
      // This should throw an exception when the date format is invalid
      encoderDecoder.decode("2023/12/25") must throwA[Exception]
    }
  }

}
