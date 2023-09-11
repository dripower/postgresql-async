package com.github.mauricio.async.db.postgresql.codec

import com.ongres.scram.client._
import com.ongres.scram.common.stringprep.StringPreparations

private[postgresql] trait ScramHandler {
  def clientFirstMsg(): (String, String)
  def clientFinalMsg(serverFirstMsg: String): String
  def verifyServerFinalMsg(serverFinalMsg: String): Either[Throwable, Unit]
}

private[postgresql] object ScramHandler {

  def apply(password: String, mechanisms: Array[String]) = new ScramHandler {
    private val _scramClient = ScramClient
      .channelBinding(ScramClient.ChannelBinding.NO)
      .stringPreparation(StringPreparations.SASL_PREPARATION)
      .selectMechanismBasedOnServerAdvertised(mechanisms: _*)
      .setup()

    private val _session = _scramClient.scramSession("*")

    def scramClient = _scramClient

    def clientFirstMsg() = {
      val fm = _session.clientFirstMessage()
      val mn = _scramClient.getScramMechanism().getName()
      (mn, fm)
    }

    def clientFinalMsg(serverFirstMsg: String) = {
      val serverFirstProcessor = _session.receiveServerFirstMessage(serverFirstMsg)
      val clientFinalProcessor = serverFirstProcessor.clientFinalProcessor(password)
      clientFinalProcessor.clientFinalMessage()
    }

  }
}
