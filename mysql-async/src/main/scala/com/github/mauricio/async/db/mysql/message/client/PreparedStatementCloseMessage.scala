package com.github.mauricio.async.db.mysql.message.client

case class PreparedStatementCloseMessage (statementId : Array[Byte])
    extends ClientMessage( ClientMessage.PreparedStatementClose )
