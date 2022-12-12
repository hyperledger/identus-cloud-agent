package io.iohk.atala.castor.core.model

import java.net.URI
import java.time.Instant
import com.google.protobuf.ByteString
import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  DIDData,
  EllipticCurve,
  InternalKeyPurpose,
  InternalPublicKey,
  LongFormPrismDID,
  PrismDID,
  PrismDIDOperation,
  PublicKey,
  PublicKeyData,
  ScheduledDIDOperationDetail,
  ScheduledDIDOperationStatus,
  Service,
  ServiceType,
  VerificationRelationship
}
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.protos.common_models.OperationStatus
import io.iohk.atala.prism.protos.node_models.KeyUsage
import io.iohk.atala.prism.protos.node_models.PublicKey.KeyData
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.utils.Traverse.*
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import zio.*

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
              publicKeys = createDIDOp.publicKeys.map(_.toProto) ++ createDIDOp.internalKeys.map(_.toProto),
              services = createDIDOp.services.map(_.toProto)
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
        // TODO: define the corresponding KeyUsage in Prism DID (ATL-2213)
        usage = publicKey.purpose match {
          case VerificationRelationship.Authentication       => node_models.KeyUsage.AUTHENTICATION_KEY
          case VerificationRelationship.AssertionMethod      => node_models.KeyUsage.ISSUING_KEY
          case VerificationRelationship.KeyAgreement         => node_models.KeyUsage.COMMUNICATION_KEY
          case VerificationRelationship.CapabilityInvocation => ???
          case VerificationRelationship.CapabilityDelegation => ???
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
        usage = internalPublicKey.purpose match {
          case InternalKeyPurpose.Master     => node_models.KeyUsage.MASTER_KEY
          case InternalKeyPurpose.Revocation => node_models.KeyUsage.REVOCATION_KEY
        },
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

  extension (service: Service) {
    def toProto: node_models.Service = node_models.Service(
      id = service.id,
      `type` = service.`type`.name,
      serviceEndpoint = service.serviceEndpoint.map(_.toString),
      addedOn = None,
      deletedOn = None
    )
  }

  extension (resp: node_api.GetOperationInfoResponse) {
    def toDomain: Either[String, Option[ScheduledDIDOperationDetail]] = {
      val status = resp.operationStatus match {
        case OperationStatus.UNKNOWN_OPERATION      => Right(None)
        case OperationStatus.PENDING_SUBMISSION     => Right(Some(ScheduledDIDOperationStatus.Pending))
        case OperationStatus.AWAIT_CONFIRMATION     => Right(Some(ScheduledDIDOperationStatus.AwaitingConfirmation))
        case OperationStatus.CONFIRMED_AND_APPLIED  => Right(Some(ScheduledDIDOperationStatus.Confirmed))
        case OperationStatus.CONFIRMED_AND_REJECTED => Right(Some(ScheduledDIDOperationStatus.Rejected))
        case OperationStatus.Unrecognized(unrecognizedValue) =>
          Left(s"unrecognized status of GetOperationInfoResponse: $unrecognizedValue")
      }
      status.map(s => s.map(ScheduledDIDOperationDetail.apply))
    }
  }

  extension (didData: node_models.DIDData) {
    def toDomain: Either[String, DIDData] = {
      for {
        canonicalDID <- PrismDID.buildCanonicalFromSuffix(didData.id)
        allKeys <- didData.publicKeys.traverse(_.toDomain)
        services <- didData.services.traverse(_.toDomain)
      } yield DIDData(
        id = canonicalDID,
        publicKeys = allKeys.collect { case key: PublicKey => key },
        internalKeys = allKeys.collect { case key: InternalPublicKey => key },
        services = services
      )
    }

    /** Return DIDData with keys and services removed by checking revocation time against the current time */
    def filterRevokedKeysAndServices: UIO[node_models.DIDData] = {
      Clock.instant.map { now =>
        didData
          .withPublicKeys(didData.publicKeys.filter { publicKey =>
            publicKey.revokedOn.flatMap(_.toInstant).forall(revokeTime => revokeTime isBefore now)
          })
          .withServices(didData.services.filter { service =>
            service.deletedOn.flatMap(_.toInstant).forall(revokeTime => revokeTime isBefore now)
          })
      }
    }
  }

  extension (ledgerData: node_models.LedgerData) {
    def toInstant: Option[Instant] = ledgerData.timestampInfo
      .flatMap(_.blockTimestamp)
      .map(ts => Instant.ofEpochSecond(ts.seconds).plusNanos(ts.nanos))
  }

  extension (operation: node_models.CreateDIDOperation) {
    def toDomain: Either[String, PrismDIDOperation.Create] = {
      for {
        allKeys <- operation.didData.map(_.publicKeys.traverse(_.toDomain)).getOrElse(Right(Nil))
        services <- operation.didData.map(_.services.traverse(_.toDomain)).getOrElse(Right(Nil))
      } yield PrismDIDOperation.Create(
        publicKeys = allKeys.collect { case key: PublicKey => key },
        internalKeys = allKeys.collect { case key: InternalPublicKey => key },
        services = services
      )
    }
  }

  extension (service: node_models.Service) {
    def toDomain: Either[String, Service] = {
      for {
        uris <- service.serviceEndpoint.traverse(s =>
          Try(URI.create(s)).toEither.left.map(_ => s"unable to parse serviceEndpoint $s as URI")
        )
        serviceType <- ServiceType
          .parseString(service.`type`)
          .toRight(s"unable to parse ${service.`type`} as service type")
      } yield Service(
        id = service.id,
        `type` = serviceType,
        serviceEndpoint = uris
      )
    }
  }

  extension (publicKey: node_models.PublicKey) {
    def toDomain: Either[String, PublicKey | InternalPublicKey] = {
      val purpose: Either[String, VerificationRelationship | InternalKeyPurpose] = publicKey.usage match {
        case node_models.KeyUsage.UNKNOWN_KEY => Left(s"unsupported use of KeyUsage.UNKNOWN_KEY on key ${publicKey.id}")
        case node_models.KeyUsage.MASTER_KEY  => Right(InternalKeyPurpose.Master)
        case node_models.KeyUsage.ISSUING_KEY => Right(VerificationRelationship.AssertionMethod)
        case node_models.KeyUsage.COMMUNICATION_KEY  => Right(VerificationRelationship.KeyAgreement)
        case node_models.KeyUsage.AUTHENTICATION_KEY => Right(VerificationRelationship.Authentication)
        case node_models.KeyUsage.REVOCATION_KEY =>
          ??? // TODO: define the corresponding KeyUsage in Prism DID (ATL-2213)
        case node_models.KeyUsage.Unrecognized(unrecognizedValue) =>
          Left(s"unrecognized KeyUsage: $unrecognizedValue on key ${publicKey.id}")
      }

      for {
        purpose <- purpose
        keyData <- publicKey.keyData.toDomain
      } yield purpose match {
        case purpose: VerificationRelationship =>
          PublicKey(
            id = publicKey.id,
            purpose = purpose,
            publicKeyData = keyData
          )
        case purpose: InternalKeyPurpose =>
          publicKey.keyData.ecKeyData
          InternalPublicKey(
            id = publicKey.id,
            purpose = purpose,
            publicKeyData = keyData
          )
      }
    }
  }

  extension (publicKeyData: node_models.PublicKey.KeyData) {
    def toDomain: Either[String, PublicKeyData] = {
      publicKeyData match {
        case KeyData.Empty => Left(s"unable to convert KeyData.Emtpy to PublicKeyData")
        case KeyData.EcKeyData(ecKeyData) =>
          for {
            curve <- EllipticCurve
              .parseString(ecKeyData.curve)
              .toRight(s"unsupported elliptic curve ${ecKeyData.curve}")
          } yield PublicKeyData.ECKeyData(
            crv = curve,
            x = Base64UrlString.fromByteArray(ecKeyData.x.toByteArray),
            y = Base64UrlString.fromByteArray(ecKeyData.y.toByteArray)
          )
        case KeyData.CompressedEcKeyData(ecKeyData) =>
          val ecPublicKey = EC.INSTANCE.toPublicKeyFromCompressed(ecKeyData.data.toByteArray)
          for {
            curve <- EllipticCurve
              .parseString(ecKeyData.curve)
              .toRight(s"unsupported elliptic curve ${ecKeyData.curve}")
          } yield PublicKeyData.ECKeyData(
            crv = curve,
            x = Base64UrlString.fromByteArray(ecPublicKey.getCurvePoint.getX.bytes()),
            y = Base64UrlString.fromByteArray(ecPublicKey.getCurvePoint.getY.bytes())
          )
      }
    }
  }

}
