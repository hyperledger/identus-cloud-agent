package io.iohk.atala.agent.walletapi.crypto

import zio.*

trait ApolloSpecHelper {
  protected val apollo: Apollo = Prism14Apollo
  protected val apolloLayer: ULayer[Apollo] = Apollo.prism14Layer
}
