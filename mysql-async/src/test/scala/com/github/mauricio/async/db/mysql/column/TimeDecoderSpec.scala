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

import java.time.Duration
import org.specs2.mutable.Specification

class TimeDecoderSpec extends Specification {

  "decoder" in {

    "handle a time" in {

      val time     = "120:10:07"
      val duration = Duration.ofHours(120).plusMinutes(10).plusSeconds(7)

      TimeDecoder.decode(time) === duration
    }

    "handle a time with microseconds" in {

      val time     = "120:10:07.00098"
      val duration = Duration
        .ofHours(120)
        .plusMinutes(10)
        .plusSeconds(7)
        .plusNanos(980000)

      TimeDecoder.decode(time) === duration

    }

    "handle a negative time" in {
      val time     = "-120:10:07.00098"
      val duration = Duration
        .ofHours(120)
        .plusMinutes(10)
        .plusSeconds(7)
        .plusNanos(980000)
        .negated()

      TimeDecoder.decode(time) === duration
    }

  }

}
