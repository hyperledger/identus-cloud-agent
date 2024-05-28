package org.hyperledger.identus.didcomm.controller

import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import zio.IO

trait DIDCommController {
  def handleDIDCommMessage(msg: String)(implicit rc: RequestContext): IO[ErrorResponse, Unit]
}
