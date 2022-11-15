package io.iohk.atala.castor.core.model

import java.net.URI
import com.google.protobuf.ByteString
import io.iohk.atala.castor.core.model.did.{
  InternalPublicKey,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  VerificationRelationship
}
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.utils.Traverse.*
import io.iohk.atala.prism.protos.node_models

import scala.util.Try

object ProtoModelHelper extends ProtoModelHelper

private[castor] trait ProtoModelHelper {

  extension (bytes: Array[Byte]) {
    def toProto: ByteString = ByteString.copyFrom(bytes)
  }

  extension (createDIDOp: PrismDIDOperation.Create) {
    def toProto: node_models.AtalaOperation.Operation.CreateDid = {
      node_models.AtalaOperation.Operation.CreateDid(
        value = node_models.CreateDIDOperation(
          didData = Some(
            node_models.CreateDIDOperation.DIDCreationData(
              publicKeys = createDIDOp.publicKeys.map(_.toProto) ++ createDIDOp.internalKeys.map(_.toProto)
            )
          )
        )
      )
    }
  }

  extension (publicKey: PublicKey) {
    def toProto: node_models.PublicKey = {
      node_models.PublicKey(
        id = publicKey.id,
        usage = publicKey.purpose match {
          case VerificationRelationship.Authentication  => node_models.KeyUsage.AUTHENTICATION_KEY
          case VerificationRelationship.AssertionMethod => node_models.KeyUsage.ISSUING_KEY
          case VerificationRelationship.KeyAgreement    => node_models.KeyUsage.COMMUNICATION_KEY
          case VerificationRelationship.CapabilityInvocation =>
            ??? // TODO: define the corresponding KeyUsage in Prism DID
        },
        addedOn = None,
        revokedOn = None,
        keyData = publicKey.publicKeyData.toProto
      )
    }
  }

  extension (internalPublicKey: InternalPublicKey) {
    def toProto: node_models.PublicKey = {
      node_models.PublicKey(
        id = internalPublicKey.id,
        usage = node_models.KeyUsage.MASTER_KEY,
        addedOn = None,
        revokedOn = None,
        keyData = internalPublicKey.publicKeyData.toProto
      )
    }
  }

  extension (publicKeyData: PublicKeyData) {
    def toProto: node_models.PublicKey.KeyData = {
      publicKeyData match {
        case PublicKeyData.ECKeyData(crv, x, y) =>
          node_models.PublicKey.KeyData.EcKeyData(
            value = node_models.ECKeyData(
              curve = crv.name,
              x = x.toByteArray.toProto,
              y = y.toByteArray.toProto
            )
          )
      }
    }
  }

}
