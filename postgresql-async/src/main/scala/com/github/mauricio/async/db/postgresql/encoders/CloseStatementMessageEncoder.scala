package com.github.mauricio.async.db.postgresql.encoders

import com.github.mauricio.async.db.postgresql.messages.frontend.{ClientMessage, CloseStatementMessage}
import io.netty.buffer.{Unpooled, ByteBuf}

object CloseStatementMessageEncoder extends Encoder {

  override def encode(message: ClientMessage): ByteBuf = {
    val closeMessage = message.asInstanceOf[CloseStatementMessage]
    val nameBytes    = closeMessage.statementName.getBytes("UTF-8")
    val restLength   = 1 + nameBytes.length + 1         // 'S' + name + null
    val totalLength  = 4 + restLength
    val buffer       = Unpooled.buffer(1 + totalLength) // message type + total
    buffer.writeByte('C')
    buffer.writeInt(totalLength)
    buffer.writeByte('S') // 'S' for statement
    buffer.writeBytes(nameBytes)
    buffer.writeByte(0) // null terminator
    buffer
  }

}
