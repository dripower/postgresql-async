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

package com.github.mauricio.async.db.postgresql.benchmark

import com.github.mauricio.async.db.postgresql.util.DateTimeParserHelper
import io.netty.buffer.{ByteBuf, Unpooled}
import java.nio.charset.StandardCharsets
import java.time.{LocalDateTime, OffsetDateTime}
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class TimestampParserBenchmark {

  private val timestampByteBuf: ByteBuf = {
    val timestampString = "2023-12-25 14:30:45.123456"
    Unpooled.wrappedBuffer(timestampString.getBytes(StandardCharsets.UTF_8))
  }
  private val timestampWithTimezoneByteBuf: ByteBuf = {
    val timestampWithTimezoneString = "2023-12-25 14:30:45.123456+05:00"
    Unpooled.wrappedBuffer(timestampWithTimezoneString.getBytes(StandardCharsets.UTF_8))
  }

  @TearDown
  def tearDown(): Unit = {
    if (timestampByteBuf != null) timestampByteBuf.release()
    if (timestampWithTimezoneByteBuf != null) timestampWithTimezoneByteBuf.release()
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  def benchmarkParseLocalDateTime(): LocalDateTime = {
    val slice = timestampByteBuf.slice()
    val bytes = Array.ofDim[Byte](slice.readableBytes())
    slice.readBytes(bytes)
    val timestampString = new String(bytes)
    DateTimeParserHelper.parseLocalDateTime(timestampString)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  def benchmarkParseOffsetDateTime(): OffsetDateTime = {
    val slice = timestampWithTimezoneByteBuf.slice()
    val bytes = Array.ofDim[Byte](slice.readableBytes())
    slice.readBytes(bytes)
    val timestampWithTimezoneString = new String(bytes)
    DateTimeParserHelper.parseOffsetDateTime(timestampWithTimezoneString)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  def benchmarkFastParseLocalDateTime(): Option[LocalDateTime] = {
    DateTimeParserHelper.fastParseLocalDateTime(timestampByteBuf.slice(), StandardCharsets.UTF_8)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  def benchmarkFastParseOffsetDateTime(): Option[OffsetDateTime] = {
    DateTimeParserHelper.fastParseOffsetDateTime(timestampWithTimezoneByteBuf.slice())
  }

}
