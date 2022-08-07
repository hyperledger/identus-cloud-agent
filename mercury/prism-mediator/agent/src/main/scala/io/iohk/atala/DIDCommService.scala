package io.iohk.atala

// import org.didcommx.didcomm.secret.SecretResolverInMemory
import org.didcommx.didcomm.message.Message
import org.didcommx.didcomm.model.PackSignedResult
import org.didcommx.didcomm.DIDComm
import org.didcommx.didcomm.model.PackSignedParams

import zio._

import io.iohk.atala.resolvers.UniversalDidResolver
import io.iohk.atala.resolvers.AliceSecretResolver
import io.iohk.atala.resolvers.MediatorSecretResolver
import org.didcommx.didcomm.model.PackEncryptedParams
import org.didcommx.didcomm.model.PackEncryptedResult
import org.didcommx.didcomm.model.UnpackParams
import io.iohk.atala.resolvers.BobSecretResolver
import org.didcommx.didcomm.model.UnpackResult
import org.didcommx.didcomm.protocols.routing.Routing

trait DIDCommService {
  def packSigned(msg: Message): UIO[PackSignedResult]
  def packEncrypted(msg: Message, to: String): UIO[PackEncryptedResult]
  def unpack(base64str: String): UIO[UnpackResult]
}

object DIDCommService {
  def packSigned(msg: Message): URIO[DIDCommService, PackSignedResult] =
    ZIO.serviceWithZIO(_.packSigned(msg))

  def packEncrypted(msg: Message, to: String): URIO[DIDCommService, PackEncryptedResult] =
    ZIO.serviceWithZIO(_.packEncrypted(msg, to))

  def unpack(base64str: String): URIO[DIDCommService, UnpackResult] =
    ZIO.serviceWithZIO(_.unpack(base64str))
}
