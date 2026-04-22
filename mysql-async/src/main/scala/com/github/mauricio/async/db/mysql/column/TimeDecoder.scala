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

package com.github.mauricio.async.db.mysql.column

import com.github.mauricio.async.db.column.ColumnDecoder
import java.time.Duration

object TimeDecoder extends ColumnDecoder {

  override def decode(value: String): Duration = {
    val isNegative = value.startsWith("-")
    val normalized = if (isNegative) value.substring(1) else value

    val pieces = normalized.split(':')

    val secondsAndMicros = pieces(2).split('.')
    val micros           = if (secondsAndMicros.length == 2) {
      secondsAndMicros(1).padTo(6, '0').take(6).toLong
    } else {
      0L
    }

    val hours   = pieces(0).toLong
    val minutes = pieces(1).toLong
    val seconds = secondsAndMicros(0).toLong

    val duration =
      Duration
        .ofHours(hours)
        .plusMinutes(minutes)
        .plusSeconds(seconds)
        .plusNanos(micros * 1000)

    if (isNegative) duration.negated() else duration
  }

}
