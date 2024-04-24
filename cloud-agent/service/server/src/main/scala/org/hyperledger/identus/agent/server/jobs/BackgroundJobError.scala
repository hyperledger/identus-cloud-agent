package org.hyperledger.identus.agent.server.jobs

import org.hyperledger.identus.mercury.HttpResponse

sealed trait BackgroundJobError

object BackgroundJobError {
  final case class InvalidState(cause: String) extends BackgroundJobError
  case object NotImplemented extends BackgroundJobError
  final case class ErrorResponseReceivedFromPeerAgent(response: HttpResponse) extends BackgroundJobError {
    override def toString: String = s"DIDComm sending error: [${response.status}] - ${response.bodyAsString}"
  }
}
