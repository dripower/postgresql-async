package com.github.mauricio.async.db.postgresql.codec

import com.ongres.scram.client.*
import com.ongres.scram.common.util.*
import java.util.Arrays
import java.security.cert.*

private[postgresql] trait ScramHandler {
  def clientFirstMsg(): (String, String)
  def clientFinalMsg(serverFirstMsg: String): String
  def verifyServerFinalMsg(serverFinalMsg: String): Unit
}

private[postgresql] object ScramHandler {

  def apply(password: String, mechanisms: Array[String], cert: Option[Certificate]): ScramHandler = new ScramHandler {

    val scramClient = {
      val base = ScramClient
        .builder()
        .advertisedMechanisms(Arrays.asList(mechanisms *))
        .username("*")
        .password(password.toCharArray())

      val builder = cert match {
        case Some(c) =>
          base.channelBinding("tls-server-end-point", getChannelBindData(c))
        case _ =>
          base
      }
      builder.build()
    }

    def clientFirstMsg() = {
      val fm = scramClient.clientFirstMessage()
      val mn = scramClient.getScramMechanism().getName()
      (mn, fm.toString())
    }

    def clientFinalMsg(serverFirstMsg: String) = {
      val serverFirstProcessor = scramClient.serverFirstMessage(serverFirstMsg)
      scramClient.clientFinalMessage().toString()
    }

    def verifyServerFinalMsg(serverFinalMsg: String) = {
      scramClient.serverFinalMessage(serverFinalMsg)
    }

    private def getChannelBindData(cert: Certificate) = {
      TlsServerEndpoint.getChannelBindingData(cert.asInstanceOf[X509Certificate])
    }

  }
}
