package org.hyperledger.identus.shared.crypto

import zio.*
import org.hyperledger.identus.shared.crypto.Apollo

trait ApolloSpecHelper {
  protected val apollo: Apollo = Apollo.default
  protected val apolloLayer: ULayer[Apollo] = Apollo.layer
}
