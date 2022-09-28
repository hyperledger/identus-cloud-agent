package io.iohk.atala.mercury

import io.iohk.atala.mercury.model.DidId

enum Agent(val id: DidId):
  case Alice extends Agent(DidId("did:example:alice"))
  case Bob extends Agent(DidId("did:example:bob"))
  case Mediator extends Agent(DidId("did:example:mediator"))
  case Charlie extends Agent(DidId(charlieFIXME))

def charlieFIXME = {
  val S =
    "eyJ0IjoiZG0iLCJzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwIiwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0"

  "did:peer:2.Ez6LSiseNCbbtmG6ascxpPvoyT8ewrWdtJZxwmPNxYAPWxzM8" +
    // ".Ez" + "6LSdJfkX7F3BJsYUwjDNeptqV4Wb9md6YWd3gCewQBycmwE" +
    ".Vz" + "6MkgLBGee6xL5KH8SZmqmKmQKS2o1qd4RG4dSmjtRGTfsxX" +
    ".SeyJ0IjoiZG0iLCJzIjoiaHR0cHM6Ly9leGFtcGxlLmNvbS9lbmRwb2ludCIsInIiOlsiZGlkOmV4YW1wbGU6c29tZW1lZGlhdG9yI3NvbWVrZXkiXSwiYSI6WyJkaWRjb21tL3YyIiwiZGlkY29tbS9haXAyO2Vudj1yZmM1ODciXX0"
  // s".S$S"

  //   org.didcommx.peerdid.PeerDIDCreator.createPeerDIDNumalgo2(
  //   List(keyAgreement).asJava,
  //   List().asJava, // List(keyAuthentication).asJava,
  //   service
  // )
}
