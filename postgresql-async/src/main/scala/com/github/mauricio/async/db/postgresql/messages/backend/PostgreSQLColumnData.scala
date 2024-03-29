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

package com.github.mauricio.async.db.postgresql.messages.backend

import com.github.mauricio.async.db.general.ColumnData
import com.google.common.cache._
import java.util.concurrent.TimeUnit

case class PostgreSQLColumnData private (
  name: String,
  tableObjectId: Int,
  columnNumber: Int,
  dataType: Int,
  dataTypeSize: Long,
  dataTypeModifier: Int,
  fieldFormat: Int
) extends ColumnData

object PostgreSQLColumnData {
  private val columnDataCache: LoadingCache[PostgreSQLColumnData, PostgreSQLColumnData] = CacheBuilder
    .newBuilder()
    .maximumSize(10000)
    .expireAfterAccess(60, TimeUnit.SECONDS)
    .build(
      new CacheLoader[PostgreSQLColumnData, PostgreSQLColumnData] {
        def load(cd: PostgreSQLColumnData) = cd
      }
    )

  def apply(
    name: String,
    tableObjectId: Int,
    columnNumber: Int,
    dataType: Int,
    dataTypeSize: Long,
    dataTypeModifier: Int,
    fieldFormat: Int
  ): PostgreSQLColumnData = {
    val cd = new PostgreSQLColumnData(
      name = name,
      tableObjectId = tableObjectId,
      columnNumber = columnNumber,
      dataType = dataType,
      dataTypeSize = dataTypeSize,
      dataTypeModifier = dataTypeModifier,
      fieldFormat = fieldFormat
    )
    columnDataCache.get(cd)
  }
}
