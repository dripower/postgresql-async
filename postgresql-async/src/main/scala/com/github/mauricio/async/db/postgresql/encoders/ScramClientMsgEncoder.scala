package com.github.mauricio.async.db.postgresql.encoders

import com.github.mauricio.async.db.postgresql.messages.frontend._
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer._
import java.nio.charset.{Charset, StandardCharsets}

class ScramClientMsgEncoder extends Encoder {

  def encode(message: ClientMessage): ByteBuf = {
    message match {
      case m: ScramClientFirstMsg => encodeFirstMsg(m)
      case m: ScramClientFinalMsg => encodeFinalMsg(m)
    }
  }

  private def encodeFinalMsg(msg: ScramClientFinalMsg): ByteBuf = {
    val finalMsgBytes = msg.finalMsg.getBytes(StandardCharsets.UTF_8)
    val bodyLen       = finalMsgBytes.size
    authMsg(bodyLen, Unpooled.wrappedBuffer(finalMsgBytes))
  }

  private def encodeFirstMsg(msg: ScramClientFirstMsg): ByteBuf = {
    val mechanismNameBytes = msg.mechanismName.getBytes(StandardCharsets.UTF_8)
    val firstMsgBytes      = msg.firstMsg.getBytes(StandardCharsets.UTF_8)
    val bodyLen            = mechanismNameBytes.size + 1 + 4 + firstMsgBytes.size
    val bodyBuf            = Unpooled.buffer(bodyLen)
    bodyBuf.writeBytes(mechanismNameBytes)
    bodyBuf.writeByte(0)
    bodyBuf.writeInt(firstMsgBytes.size)
    bodyBuf.writeBytes(firstMsgBytes)
    authMsg(bodyLen, bodyBuf)
  }

  private def authMsg(bodyLen: Int, body: ByteBuf) = {
    val lenBuf = Unpooled.wrappedBuffer(Array.ofDim[Byte](5))
    lenBuf.writerIndex(0)
    lenBuf.writeByte(ServerMessage.PasswordMessage)
    lenBuf.writeInt(bodyLen + 4)
    Unpooled.wrappedBuffer(lenBuf, body)
  }

}
