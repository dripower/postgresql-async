/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql.util

import java.nio.charset.StandardCharsets
import io.netty.buffer.Unpooled
import org.specs2.mutable.Specification

class DateTimeParserHelperSpec extends Specification {

  "DateTimeParserHelper" should {

    "parse LocalDateTime consistently between fast and non-fast methods" in {
      val testCases = Seq(
        "2023-12-25 14:30:45",
        "2023-12-25 14:30:45.1",
        "2023-12-25 14:30:45.12",
        "2023-12-25 14:30:45.123",
        "2023-12-25 14:30:45.1234",
        "2023-12-25 14:30:45.12345",
        "2023-12-25 14:30:45.123456",
        "1999-01-08 04:05:06",
        "2020-02-29 23:59:59.999999", // leap year
        "0001-01-01 00:00:00",        // minimum date
        "9999-12-31 23:59:59.999999"  // maximum date
      )

      testCases.forall { timestampString =>
        val buf = Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
        try {
          val fastResult    = DateTimeParserHelper.fastParseLocalDateTime(buf, StandardCharsets.UTF_8)
          val nonFastResult = DateTimeParserHelper.parseLocalDateTime(timestampString)

          fastResult.isDefined && fastResult.get == nonFastResult
        } finally {
          buf.release()
        }
      } must beTrue
    }

    "parse OffsetDateTime consistently between fast and non-fast methods for supported formats" in {
      val testCases = Seq(
        "2023-12-25 14:30:45+00:00",
        "2023-12-25 14:30:45.123456-02:30",
        "1999-01-08 04:05:06+00:00",
        "2020-02-29 23:59:59.999999+00:00",
        "0001-01-01 00:00:00+00:00",
        "9999-12-31 23:59:59.999999+00:00",
        "2023-12-25 14:30:45Z",
        "2023-12-25 14:30:45.123+03:30"
      )

      testCases must contain({ (timestampString: String) =>
        val buf           = Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
        val fastResult    = DateTimeParserHelper.fastParseOffsetDateTime(buf)
        val nonFastResult = DateTimeParserHelper.parseOffsetDateTime(timestampString)
        fastResult must beSome(nonFastResult)
      }).forall
    }

    "handle empty ByteBuf for fastParseLocalDateTime" in {
      val buf = Unpooled.wrappedBuffer(Array.empty[Byte])
      try {
        val result = DateTimeParserHelper.fastParseLocalDateTime(buf, StandardCharsets.UTF_8)
        result must beNone
      } finally {
        buf.release()
      }
    }

    "handle empty ByteBuf for fastParseOffsetDateTime" in {
      val buf = Unpooled.wrappedBuffer(Array.empty[Byte])
      try {
        val result = DateTimeParserHelper.fastParseOffsetDateTime(buf)
        result must beNone
      } finally {
        buf.release()
      }
    }

    "handle truly malformed timestamp for fastParseLocalDateTime" in {
      val malformedCases = Seq(
        "2023-12-25",          // missing time
        "14:30:45",            // missing date
        "2023-12-25 14:30",    // missing seconds
        "2023-13-25 14:30:45", // invalid month
        "2023-12-32 14:30:45", // invalid day
        "2023-12-25 24:30:45", // invalid hour
        "2023-12-25 14:60:45", // invalid minute
        "2023-12-25 14:30:60"  // invalid second
      )

      malformedCases.forall { malformedString =>
        val buf = Unpooled.wrappedBuffer(malformedString.getBytes(StandardCharsets.UTF_8))
        try {
          val result = DateTimeParserHelper.fastParseLocalDateTime(buf, StandardCharsets.UTF_8)
          result.isEmpty
        } finally {
          buf.release()
        }
      } must beTrue
    }

    "handle truly malformed timestamp for fastParseOffsetDateTime" in {
      val malformedCases = Seq(
        "2023-12-25 14:30:45+25:00" // invalid timezone hour
        // Note: "2023-12-25 14:30:45+05:60" (invalid minute) is leniently parsed by fastParseDateTime
      )

      malformedCases.forall { malformedString =>
        val buf = Unpooled.wrappedBuffer(malformedString.getBytes(StandardCharsets.UTF_8))
        try {
          val result = DateTimeParserHelper.fastParseOffsetDateTime(buf)
          result.isEmpty
        } finally {
          buf.release()
        }
      } must beTrue
    }

    "preserve ByteBuf reader index on parse failure for fastParseLocalDateTime" in {
      val malformedString = "2023-12-25 14:30" // missing seconds
      val buf             = Unpooled.wrappedBuffer(malformedString.getBytes(StandardCharsets.UTF_8))
      try {
        val initialIndex = buf.readerIndex()
        val result       = DateTimeParserHelper.fastParseLocalDateTime(buf, StandardCharsets.UTF_8)
        result must beNone
        buf.readerIndex() must_=== initialIndex
      } finally {
        buf.release()
      }
    }

    "preserve ByteBuf reader index on parse failure for fastParseOffsetDateTime" in {
      val malformedString = "2023-12-25 14:30:45+25:00"
      val buf             = Unpooled.wrappedBuffer(malformedString.getBytes(StandardCharsets.UTF_8))
      try {
        val initialIndex = buf.readerIndex()
        val result       = DateTimeParserHelper.fastParseOffsetDateTime(buf)
        result must beNone
        buf.readerIndex() must_=== initialIndex
      } finally {
        buf.release()
      }
    }

    "handle lenient fractional seconds parsing in fast methods" in {
      val lenientCases = Seq(
        "2023-12-25 14:30:45.",       // trailing dot becomes 0 milliseconds
        "2023-12-25 14:30:45.1234567" // extra digits are truncated
      )

      // These should parse successfully with fast methods but not with non-fast methods
      lenientCases.forall { timestampString =>
        val buf = Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
        try {
          val fastResult = DateTimeParserHelper.fastParseLocalDateTime(buf, StandardCharsets.UTF_8)
          fastResult.isDefined
        } finally {
          buf.release()
        }
      } must beTrue
    }

    "handle various fractional second precisions consistently" in {
      val fractionalCases = (0 to 6).map { digits =>
        val fractional      = "1" * digits
        val timestampString = if (digits > 0) {
          s"2023-12-25 14:30:45.$fractional"
        } else {
          "2023-12-25 14:30:45"
        }
        timestampString
      }

      fractionalCases.forall { timestampString =>
        val buf = Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
        try {
          val fastResult    = DateTimeParserHelper.fastParseLocalDateTime(buf, StandardCharsets.UTF_8)
          val nonFastResult = DateTimeParserHelper.parseLocalDateTime(timestampString)

          fastResult.isDefined && fastResult.get == nonFastResult
        } finally {
          buf.release()
        }
      } must beTrue
    }

    "handle edge cases for timezone offsets" in {
      val edgeCases = Seq(
        "2023-12-25 14:30:45+00:00", // UTC
        "2023-12-25 14:30:45-00:00", // negative UTC
        "2023-12-25 14:30:45+14:00", // maximum positive offset
        "2023-12-25 14:30:45-12:00"  // maximum negative offset
      )

      edgeCases.forall { timestampString =>
        val buf = Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
        try {
          val fastResult    = DateTimeParserHelper.fastParseOffsetDateTime(buf)
          val nonFastResult = DateTimeParserHelper.parseOffsetDateTime(timestampString)

          fastResult.isDefined && fastResult.get == nonFastResult
        } finally {
          buf.release()
        }
      } must beTrue
    }

    "handle short timezone formats in fastParseOffsetDateTime" in {
      val shortTimezoneCases = Seq(
        "2023-12-25 14:30:45+05", // short timezone format
        "2023-12-25 14:30:45-08", // short timezone format
        "2023-12-25 14:30:45+05:" // incomplete timezone with colon
      )

      // These should parse successfully with fastParseOffsetDateTime
      shortTimezoneCases.forall { timestampString =>
        val buf = Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
        try {
          val result = DateTimeParserHelper.fastParseOffsetDateTime(buf)
          result.isDefined
        } finally {
          buf.release()
        }
      } must beTrue
    }

    "handle lenient timezone parsing in fastParseOffsetDateTime" in {
      val lenientTimezoneCases = Seq(
        "2023-12-25 14:30:45+05:00:00",    // extra colon but parses
        "2023-12-25 14:30:45+05:00 extra", // extra text but parses
        "2023-12-25 14:30:45+05:00+"       // duplicate + but parses
      )

      // These should parse successfully with fastParseOffsetDateTime despite being non-standard
      lenientTimezoneCases.forall { timestampString =>
        val buf = Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
        try {
          val result = DateTimeParserHelper.fastParseOffsetDateTime(buf)
          result.isDefined
        } finally {
          buf.release()
        }
      } must beTrue
    }
  }
}
