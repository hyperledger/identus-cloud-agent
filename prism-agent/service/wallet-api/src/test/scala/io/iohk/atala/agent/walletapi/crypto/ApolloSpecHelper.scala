package io.iohk.atala.agent.walletapi.crypto

import zio.*
import io.iohk.atala.agent.walletapi.crypto.{Apollo, Prism14Apollo}

trait ApolloSpecHelper {
  protected val apollo: Apollo = Prism14Apollo
  protected val apolloLayer: ULayer[Apollo] = Apollo.prism14Layer
}
