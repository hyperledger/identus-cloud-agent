package io.iohk.atala.mercury

import io.iohk.atala.mercury.model.DidId

enum Agent(val id: DidId):
  case Alice extends Agent(DidId("did:example:alice"))
  case Bob extends Agent(DidId("did:example:bob"))
  case Mediator extends Agent(DidId("did:example:mediator"))
  case Charlie extends Agent(DidId("did:peer:????")) // FIXME
