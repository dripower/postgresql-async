package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage

case class ScramClientFirstMsg(
  mechanismName: String,
  firstMsg: String
) extends ClientMessage(ServerMessage.PasswordMessage)

case class ScramClientFinalMsg(
  finalMsg: String
) extends ClientMessage(ServerMessage.PasswordMessage)
