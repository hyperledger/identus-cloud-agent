package io.iohk.atala.castor.core.model

import java.net.URI
import com.google.protobuf.ByteString
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.utils.Traverse.*
import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  DIDStatePatch,
  DIDStorage,
  EllipticCurve,
  PublicKey,
  PublicKeyJwk,
  PublishedDIDOperation,
  PublishedDIDOperationOutcome,
  Service,
  ServiceType,
  VerificationRelationship
}
import io.iohk.atala.iris.proto as iris_proto
import io.iohk.atala.iris.proto.did_operations.DocumentDefinition.PublicKey.Purpose
import io.iohk.atala.iris.proto.did_operations.DocumentDefinition.Service.Type
import io.iohk.atala.iris.proto.did_operations.PublicKeyJwk.{Curve, Key}

import scala.util.Try

private[castor] object ProtoModelHelper extends ProtoModelHelper

private[castor] trait ProtoModelHelper {

  extension (bytes: Array[Byte]) {
    def toProto: ByteString = ByteString.copyFrom(bytes)
  }

  extension (operation: PublishedDIDOperation.Create) {
    def toProto: iris_proto.did_operations.CreateDid = {
      iris_proto.did_operations.CreateDid(
        initialUpdateCommitment = operation.updateCommitment.toByteArray.toProto,
        initialRecoveryCommitment = operation.recoveryCommitment.toByteArray.toProto,
        ledger = operation.storage.ledgerName,
        document = Some(operation.document.toProto)
      )
    }
  }

  extension (operation: PublishedDIDOperation.Update) {
    def toProto: iris_proto.did_operations.UpdateDid = {
      iris_proto.did_operations.UpdateDid(
        did = operation.did.toString,
        ledger = operation.did.network,
        revealedUpdateKey = operation.updateKey.toByteArray.toProto,
        previousVersion = operation.previousVersion.toByteArray.toProto,
        forwardUpdateCommitment = operation.delta.updateCommitment.toByteArray.toProto,
        patches = operation.delta.patches.map(_.toProto),
        signature = operation.signature.toByteArray.toProto
      )
    }
  }

  extension (patch: DIDStatePatch) {
    def toProto: iris_proto.did_operations.UpdateDid.Patch = {
      import iris_proto.did_operations.UpdateDid.Patch.Patch
      iris_proto.did_operations.UpdateDid.Patch(
        patch = patch match {
          case p: DIDStatePatch.AddPublicKey    => Patch.AddPublicKey(p.publicKey.toProto)
          case p: DIDStatePatch.RemovePublicKey => Patch.RemovePublicKey(p.id)
          case p: DIDStatePatch.AddService      => Patch.AddService(p.service.toProto)
          case p: DIDStatePatch.RemoveService   => Patch.RemoveService(p.id)
        }
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

  extension (op: iris_proto.did_operations.CreateDid) {
    def toDomain: Either[String, PublishedDIDOperation.Create] =
      for {
        document <- op.document.toRight("expected a DIDDocument in the protobuf message").flatMap(_.toDomain)
      } yield PublishedDIDOperation.Create(
        updateCommitment = HexString.fromByteArray(op.initialUpdateCommitment.toByteArray),
        recoveryCommitment = HexString.fromByteArray(op.initialRecoveryCommitment.toByteArray),
        storage = DIDStorage.Cardano(op.ledger),
        document = document
      )
  }

  extension (doc: iris_proto.did_operations.DocumentDefinition) {
    def toDomain: Either[String, DIDDocument] =
      for {
        publicKeys <- doc.publicKeys.traverse(_.toDomain)
        services <- doc.services.traverse(_.toDomain)
      } yield DIDDocument(
        publicKeys = publicKeys,
        services = services
      )
  }

  extension (service: iris_proto.did_operations.DocumentDefinition.Service) {
    def toDomain: Either[String, Service] = {
      for {
        serviceType <- service.`type`.toDomain
        serviceEndpoint <- Try(URI.create(service.serviceEndpoint)).toEither.left.map(_ =>
          s"unable to parse serviceEndpoint ${service.serviceEndpoint} as URI"
        )
      } yield Service(
        id = service.id,
        `type` = serviceType,
        serviceEndpoint = serviceEndpoint
      )
    }
  }

  extension (serviceType: iris_proto.did_operations.DocumentDefinition.Service.Type) {
    def toDomain: Either[String, ServiceType] = {
      serviceType match {
        case Type.MEDIATOR_SERVICE    => Right(ServiceType.MediatorService)
        case Type.Unrecognized(value) => Left(s"unrecognized serviceType value $value")
      }
    }
  }

  extension (publicKey: iris_proto.did_operations.DocumentDefinition.PublicKey) {
    def toDomain: Either[String, PublicKey] =
      for {
        purposes <- publicKey.purposes.traverse(_.toDomain)
        publicKeysJwk <- publicKey.jwk
          .toRight(s"publicKeyJwk does not exist on key id ${publicKey.id}")
          .flatMap(_.toDomain)
      } yield PublicKey.JsonWebKey2020(
        id = publicKey.id,
        purposes = purposes,
        publicKeyJwk = publicKeysJwk
      )
  }

  extension (purpose: iris_proto.did_operations.DocumentDefinition.PublicKey.Purpose) {
    def toDomain: Either[String, VerificationRelationship] = {
      purpose match {
        case Purpose.AUTHENTICATION        => Right(VerificationRelationship.Authentication)
        case Purpose.KEY_AGREEMENT         => Right(VerificationRelationship.KeyAgreement)
        case Purpose.ASSERTION_METHOD      => Right(VerificationRelationship.AssertionMethod)
        case Purpose.CAPABILITY_INVOCATION => Right(VerificationRelationship.CapabilityInvocation)
        case Purpose.Unrecognized(value)   => Left(s"unrecognized publicKey purpose $value")
      }
    }
  }

  extension (jwk: iris_proto.did_operations.PublicKeyJwk) {
    def toDomain: Either[String, PublicKeyJwk] = {
      val errorOrEcKey = jwk.key match {
        case Key.Empty        => Left("publicKeyJwk value does not exist")
        case Key.EcKey(value) => Right(value)
      }
      for {
        ecKey <- errorOrEcKey
        curve <- ecKey.curve.toDomain
      } yield PublicKeyJwk.ECPublicKeyData(
        crv = curve,
        x = Base64UrlString.fromByteArray(ecKey.x.toByteArray),
        y = Base64UrlString.fromByteArray(ecKey.y.toByteArray)
      )
    }
  }

  extension (curve: iris_proto.did_operations.PublicKeyJwk.Curve) {
    def toDomain: Either[String, EllipticCurve] = curve match {
      case Curve.SECP256K1           => Right(EllipticCurve.SECP256K1)
      case Curve.Unrecognized(value) => Left(s"unrecognized elliptic-curve value $value")
    }
  }

}
