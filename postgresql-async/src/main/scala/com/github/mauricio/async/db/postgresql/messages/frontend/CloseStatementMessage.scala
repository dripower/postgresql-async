package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage

class CloseStatementMessage(val statementName: String) extends ClientMessage(ServerMessage.CloseStatementOrPortal) {

  override def toString: String = s"CloseStatementMessage($statementName)"

}
