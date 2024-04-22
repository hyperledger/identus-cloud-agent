package io.iohk.atala.shared.crypto

import zio.*

trait ApolloSpecHelper {
  protected val apollo: Apollo = Apollo.default
  protected val apolloLayer: ULayer[Apollo] = Apollo.layer
}
