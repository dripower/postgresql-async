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

package com.github.mauricio.async.db

import com.github.mauricio.async.db.util.NettyUtils
import java.nio.charset.Charset
import io.netty.buffer.{ByteBufAllocator, PooledByteBufAllocator}
import io.netty.channel.EventLoopGroup
import io.netty.util.CharsetUtil
import io.netty.channel.socket.SocketChannel
import scala.concurrent.duration._
import scala.language.existentials

object Configuration {
  val DefaultCharset = CharsetUtil.UTF_8

  @deprecated(
    "Use com.github.mauricio.async.db.postgresql.util.URLParser.DEFAULT or com.github.mauricio.async.db.mysql.util.URLParser.DEFAULT.",
    since = "0.2.20"
  )
  val Default = new Configuration("postgres")
}

/**
 * Contains the configuration necessary to connect to a database.
 *
 * @param username
 *   database username
 * @param host
 *   database host, defaults to "localhost"
 * @param port
 *   database port, defaults to 5432
 * @param password
 *   password, defaults to no password
 * @param database
 *   database name, defaults to no database
 * @param ssl
 *   ssl configuration
 * @param charset
 *   charset for the connection, defaults to UTF-8, make sure you know what you are doing if you change this
 * @param maximumMessageSize
 *   the maximum size a message from the server could possibly have, this limits possible OOM or eternal loop attacks
 *   the client could have, defaults to 16 MB. You can set this to any value you would like but again, make sure you
 *   know what you are doing if you do change it.
 * @param allocator
 *   the netty buffer allocator to be used
 * @param connectTimeout
 *   the timeout for connecting to servers
 * @param testTimeout
 *   the timeout for connection tests performed by pools
 * @param queryTimeout
 *   the optional query timeout
 */
case class Configuration(
  username: String,
  host: String = "localhost",
  port: Int = 5432,
  password: Option[String] = None,
  database: Option[String] = None,
  ssl: SSLConfiguration = SSLConfiguration(),
  charset: Charset = Configuration.DefaultCharset,
  maximumMessageSize: Int = 16777216,
  allocator: ByteBufAllocator = PooledByteBufAllocator.DEFAULT,
  connectTimeout: Duration = 5.seconds,
  testTimeout: Duration = 5.seconds,
  preparedStatementCacheSize: Int = 64,
  preparedStatementExpireTime: Duration = 60.seconds,
  eventLoopGroup: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
  channelClass: Class[_ <: SocketChannel] = NettyUtils.SocketChannelClass,
  queryTimeout: Option[Duration] = None
)
