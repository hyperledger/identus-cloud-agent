package io.iohk.atala.resolvers

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import org.didcommx.peerdid.*

import scala.jdk.CollectionConverters.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import org.didcommx.peerdid.core.PeerDIDUtils

object PeerDidMediatorSecretResolver {

  @main def mediatorPeerDidDoc(): Unit = {
    val peerDidMediator = io.iohk.atala.mercury.PeerDID.makePeerDid()
    val secretResolver = peerDidMediator.getSecretResolverInMemory
    println(peerDidMediator.getDIDDocument)
  }

}
