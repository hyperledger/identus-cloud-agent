package io.iohk.atala.castor.core.model

import com.google.protobuf.ByteString
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  EllipticCurve,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation,
  Service,
  ServiceType,
  VerificationRelationship
}
import io.iohk.atala.iris.proto as iris_proto

private[castor] trait ProtoModelHelper {

  extension (bytes: Array[Byte]) {
    def toProto: ByteString = ByteString.copyFrom(bytes)
  }

  extension (operation: PublishedDIDOperation.Create) {
    def toProto: iris_proto.dlt.IrisOperation = {
      iris_proto.dlt.IrisOperation(
        operation = iris_proto.dlt.IrisOperation.Operation.CreateDid(
          value = iris_proto.did_operations.CreateDid(
            initialUpdateCommitment = operation.updateCommitment.toByteArray.toProto,
            initialRecoveryCommitment = operation.recoveryCommitment.toByteArray.toProto,
            ledger = operation.storage.ledgerName,
            document = Some(operation.document.toProto)
          )
        )
      )
    }
  }

  extension (doc: DIDDocument) {
    def toProto: iris_proto.did_operations.DocumentDefinition = {
      iris_proto.did_operations.DocumentDefinition(
        publicKeys = doc.publicKeys.map(_.toProto),
        services = doc.services.map(_.toProto)
      )
    }
  }

  extension (service: Service) {
    def toProto: iris_proto.did_operations.DocumentDefinition.Service = {
      iris_proto.did_operations.DocumentDefinition.Service(
        id = service.id,
        `type` = service.`type`.toProto,
        serviceEndpoint = service.serviceEndpoint.toString
      )
    }
  }

  extension (key: PublicKey) {
    def toProto: iris_proto.did_operations.DocumentDefinition.PublicKey = {
      key match {
        case k: PublicKey.JsonWebKey2020 =>
          iris_proto.did_operations.DocumentDefinition.PublicKey(
            id = k.id,
            jwk = Some(k.publicKeyJwk.toProto),
            purposes = k.purposes.map(_.toProto)
          )
      }
    }
  }

  extension (serviceType: ServiceType) {
    def toProto: iris_proto.did_operations.DocumentDefinition.Service.Type = {
      import iris_proto.did_operations.DocumentDefinition.Service.Type.*
      serviceType match {
        case ServiceType.MediatorService => MEDIATOR_SERVICE
      }
    }
  }

  extension (purpose: VerificationRelationship) {
    def toProto: iris_proto.did_operations.DocumentDefinition.PublicKey.Purpose = {
      import iris_proto.did_operations.DocumentDefinition.PublicKey.Purpose.*
      purpose match {
        case VerificationRelationship.Authentication       => AUTHENTICATION
        case VerificationRelationship.AssertionMethod      => ASSERTION_METHOD
        case VerificationRelationship.KeyAgreement         => KEY_AGREEMENT
        case VerificationRelationship.CapabilityInvocation => CAPABILITY_INVOCATION
      }
    }
  }

  extension (key: PublicKeyJwk) {
    def toProto: iris_proto.did_operations.PublicKeyJwk = {
      key match {
        case k: PublicKeyJwk.ECPublicKeyData =>
          iris_proto.did_operations.PublicKeyJwk(
            key = iris_proto.did_operations.PublicKeyJwk.Key.EcKey(
              iris_proto.did_operations.PublicKeyJwk.ECKeyData(
                curve = k.crv.toProto,
                x = k.x.toByteArray.toProto,
                y = k.y.toByteArray.toProto
              )
            )
          )
      }
    }
  }

  extension (curve: EllipticCurve) {
    def toProto: iris_proto.did_operations.PublicKeyJwk.Curve = {
      import iris_proto.did_operations.PublicKeyJwk.Curve.*
      curve match {
        case EllipticCurve.SECP256K1 => SECP256K1
      }
    }
  }

}
