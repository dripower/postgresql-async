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

package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.postgresql.messages.backend.PostgreSQLColumnData
import com.google.common.cache._
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ArrayBuffer

class PreparedStatementHolder private (
  val query: String,
  val statementId: Int,
  isPositional: Boolean = false
) {

  private def PostionalMarkPattern = "\\$\\d+".r

  lazy val (realQuery, paramsCount) = {
    if (!isPositional) {
      val result = new StringBuilder(query.length + 16)
      var offset = 0
      var params = 0
      while (offset < query.length) {
        val next = query.indexOf('?', offset)
        if (next == -1) {
          result ++= query.substring(offset)
          offset = query.length
        } else {
          result ++= query.substring(offset, next)
          offset = next + 1
          if (offset < query.length && query(offset) == '?') {
            result += '?'
            offset += 1
          } else {
            result += '$'
            params += 1
            result ++= params.toString
          }
        }
      }
      (result.toString, params)
    } else {
      (query, PostionalMarkPattern.findAllIn(query).size)
    }
  }

  var prepared: Boolean                              = false
  var columnDatas: ArrayBuffer[PostgreSQLColumnData] = ArrayBuffer.empty

}

object PreparedStatementHolder {

  private val queryCache: LoadingCache[String, String] = CacheBuilder
    .newBuilder()
    .maximumSize(10000)
    .expireAfterAccess(60, TimeUnit.SECONDS)
    .build(
      new CacheLoader[String, String] {
        def load(k: String) = k
      }
    )

  def apply(
    query: String,
    statementId: Int,
    isPositional: Boolean = false
  ): PreparedStatementHolder = {
    new PreparedStatementHolder(queryCache.get(query), statementId, isPositional)
  }
}
