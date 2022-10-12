package io.iohk.atala.mercury.resolvers

import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import io.iohk.atala.resolvers.PeerDidMediatorDidDoc
import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import org.didcommx.peerdid.*
import org.didcommx.peerdid.core.PeerDIDUtils
import io.iohk.atala.mercury.PeerDID

import scala.jdk.CollectionConverters.*

object PeerDidMediatorSecretResolver {

  val peer = PeerDID.makePeerDid()

}
