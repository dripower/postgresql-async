package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage

sealed abstract class ScramClientMsg extends ClientMessage(ServerMessage.PasswordMessage)

case class ScramClientFirstMsg(
  mechanismName: String,
  firstMsg: String
) extends ScramClientMsg

case class ScramClientFinalMsg(
  finalMsg: String
) extends ScramClientMsg
