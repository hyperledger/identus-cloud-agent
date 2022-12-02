package io.iohk.atala.api.http

import sttp.tapir.model.ServerRequest

case class RequestContext(request: ServerRequest)
