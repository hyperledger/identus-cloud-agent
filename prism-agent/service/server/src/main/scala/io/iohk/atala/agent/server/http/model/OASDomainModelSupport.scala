package io.iohk.atala.agent.server.http.model

import io.iohk.atala.agent.openapi.model.{CreateDIDRequest, JsonWebKey2020, PublicKey, PublicKeyJwk, Service}
import io.iohk.atala.castor.core.model.did as domain
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.utils.Traverse.*

import java.net.URI
import scala.util.Try

trait OASDomainModelSupport {

  private val verificationRelationshipLookup = domain.VerificationRelationship.values.map(i => i.name -> i).toMap

  private val ellipticCurveLookup = domain.EllipticCurve.values.map(i => i.name -> i).toMap

  private val serviceTypeLookup = domain.ServiceType.values.map(i => i.name -> i).toMap

  /** Only provide support for PublishedDID until other types of DID has been approved. Eventually this conversion
    * should check for `didType` and convert to the right operation on a given `didType`
    */
  extension (req: CreateDIDRequest) {
    def toDomain: Either[String, domain.PublishedDIDOperation.Create] = {
      for {
        updateCommitmentHex <- HexString
          .fromString(req.updateCommitment)
          .toEither
          .left
          .map(_ => "unable to convert updateCommitment to hex string")
        recoveryCommitmentHex <- HexString
          .fromString(req.recoveryCommitment)
          .toEither
          .left
          .map(_ => "unable to convert recoveryCommitment to hex string")
        publicKeys <- req.document.publicKeys.getOrElse(Nil).traverse(_.toDomain)
        services <- req.document.services.getOrElse(Nil).traverse(_.toDomain)
      } yield domain.PublishedDIDOperation.Create(
        updateCommitment = updateCommitmentHex,
        recoveryCommitment = recoveryCommitmentHex,
        storage = domain.DIDStorage.Cardano(req.storage),
        document = domain.DIDDocument(publicKeys = publicKeys, services = services)
      )
    }
  }

  extension (service: Service) {
    def toDomain: Either[String, domain.Service] = {
      for {
        serviceEndpoint <- Try(URI.create(service.serviceEndpoint)).toEither.left.map(_ =>
          s"unable to parse serviceEndpoint ${service.serviceEndpoint} as URI"
        )
        serviceType <- serviceTypeLookup.get(service.`type`).toRight(s"unsupported serviceType ${service.`type`}")
      } yield domain.Service(
        id = service.id,
        `type` = serviceType,
        serviceEndpoint = serviceEndpoint
      )
    }
  }

  extension (key: PublicKey) {
    def toDomain: Either[String, domain.PublicKey] = {
      for {
        purposes <- key.purposes.traverse(i =>
          verificationRelationshipLookup
            .get(i)
            .toRight(s"unsupported verificationRelationship $i")
        )
        publicKeyJwk <- key.jsonWebKey2020.publicKeyJwk.toDomain
      } yield domain.PublicKey.JsonWebKey2020(id = key.id, purposes = purposes, publicKeyJwk = publicKeyJwk)
    }
  }

  extension (jwk: PublicKeyJwk) {
    def toDomain: Either[String, domain.PublicKeyJwk] = {
      for {
        crv <- jwk.crv
          .toRight("expected crv field in JWK")
          .flatMap(i => ellipticCurveLookup.get(i).toRight(s"unsupported curve $i"))
        x <- jwk.x
          .toRight("expected x field in JWK")
          .flatMap(
            Base64UrlString.fromString(_).toEither.left.map(_ => "unable to convert x coordinate to base64url string")
          )
        y <- jwk.y
          .toRight("expected y field in JWK")
          .flatMap(
            Base64UrlString.fromString(_).toEither.left.map(_ => "unable to convert y coordinate to base64url string")
          )
      } yield domain.PublicKeyJwk.ECPublicKeyData(crv = crv, x = x, y = y)
    }
  }

}
