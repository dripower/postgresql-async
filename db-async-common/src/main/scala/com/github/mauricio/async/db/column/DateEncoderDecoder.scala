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

package com.github.mauricio.async.db.column

import org.joda.time.{ReadablePartial, LocalDate}
import com.github.mauricio.async.db.exceptions.DateEncoderNotAvailableException

object DateEncoderDecoder extends ColumnEncoderDecoder {

  private val ZeroedDate = "0000-00-00"

  override def decode(value: String): LocalDate =
    if (ZeroedDate == value) {
      null
    } else {
      parseDate(value)
    }

  override def encode(value: Any): String = {
    value match {
      case d: java.sql.Date   => formatDate(new LocalDate(d))
      case d: ReadablePartial => formatDate(d)
      case _                  => throw new DateEncoderNotAvailableException(value)
    }
  }

  /**
   * Optimized date parsing using manual parsing instead of formatter for better performance
   */
  private def parseDate(value: String): LocalDate = {
    if (value.length != 10 || value.charAt(4) != '-' || value.charAt(7) != '-') {
      oldFormatter.parseLocalDate(value)
    } else {
      val yearOpt  = value.slice(0, 4).toIntOption
      val monthOpt = value.slice(5, 7).toIntOption
      val dayOpt   = value.slice(8, 10).toIntOption

      (yearOpt, monthOpt, dayOpt) match {
        case (Some(year), Some(month), Some(day)) =>
          new LocalDate(year, month, day)
        case _ =>
          // Fallback to old formatter on any parsing error
          oldFormatter.parseLocalDate(value)
      }
    }
  }

  /**
   * Optimized date formatting using manual formatting instead of formatter for better performance
   */
  private def formatDate(date: ReadablePartial): String = {
    val year  = date.get(org.joda.time.DateTimeFieldType.year())
    val month = date.get(org.joda.time.DateTimeFieldType.monthOfYear())
    val day   = date.get(org.joda.time.DateTimeFieldType.dayOfMonth())

    // Use string interpolation for cleaner formatting
    f"$year%04d-$month%02d-$day%02d"
  }

  // Keep old formatter for fallback
  private val oldFormatter = org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd")

}
