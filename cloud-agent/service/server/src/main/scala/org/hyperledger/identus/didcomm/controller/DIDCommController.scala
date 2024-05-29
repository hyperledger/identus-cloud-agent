package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.didcomm.controller.http.DIDCommMessage
import zio.IO

trait DIDCommController {
  def handleDIDCommMessage(msg: DIDCommMessage)(implicit rc: RequestContext): IO[ErrorResponse, Unit]
}
