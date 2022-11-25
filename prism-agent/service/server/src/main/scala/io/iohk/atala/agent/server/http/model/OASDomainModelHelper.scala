package io.iohk.atala.agent.server.http.model

import io.iohk.atala.agent.openapi.model.{
  CreateDIDRequest,
  CreateManagedDidRequestDocumentTemplate,
  CreateManagedDidRequestDocumentTemplatePublicKeysInner,
  DIDOperationResponse,
  DidOperation,
  DidOperationSubmission,
  JsonWebKey2020,
  PublicKey,
  PublicKeyJwk,
  Service
}
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.castor.core.model.did.PublishedDIDOperation
import io.iohk.atala.agent.walletapi.model as walletDomain
import io.iohk.atala.pollux.core.model as polluxdomain
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.models.Base64UrlStrings.*
import io.iohk.atala.shared.utils.Traverse.*

import java.net.URI
import scala.util.Try
import io.iohk.atala.agent.openapi.model.IssueCredentialRecord
import io.iohk.atala.agent.openapi.model.IssueCredentialRecordCollection
import java.time.OffsetDateTime
import java.time.ZoneOffset
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.Base64

trait OASDomainModelHelper {

  extension (req: CreateDIDRequest) {
    def toDomain: Either[String, castorDomain.PublishedDIDOperation.Create] = {
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
      } yield castorDomain.PublishedDIDOperation.Create(
        updateCommitment = updateCommitmentHex,
        recoveryCommitment = recoveryCommitmentHex,
        storage = castorDomain.DIDStorage.Cardano(req.storage),
        document = castorDomain.DIDDocument(publicKeys = publicKeys, services = services)
      )
    }
  }

  extension (service: Service) {
    def toDomain: Either[String, castorDomain.Service] = {
      for {
        serviceEndpoint <- Try(URI.create(service.serviceEndpoint)).toEither.left.map(_ =>
          s"unable to parse serviceEndpoint ${service.serviceEndpoint} as URI"
        )
        serviceType <- castorDomain.ServiceType
          .parseString(service.`type`)
          .toRight(s"unsupported serviceType ${service.`type`}")
      } yield castorDomain.Service(
        id = service.id,
        `type` = serviceType,
        serviceEndpoint = serviceEndpoint
      )
    }
  }

  extension (key: PublicKey) {
    def toDomain: Either[String, castorDomain.PublicKey] = {
      for {
        purposes <- key.purposes.traverse(i =>
          castorDomain.VerificationRelationship
            .parseString(i)
            .toRight(s"unsupported verificationRelationship $i")
        )
        publicKeyJwk <- key.jsonWebKey2020.publicKeyJwk.toDomain
      } yield castorDomain.PublicKey.JsonWebKey2020(id = key.id, purposes = purposes, publicKeyJwk = publicKeyJwk)
    }
  }

  extension (jwk: PublicKeyJwk) {
    def toDomain: Either[String, castorDomain.PublicKeyJwk] = {
      for {
        crv <- jwk.crv
          .toRight("expected crv field in JWK")
          .flatMap(i => castorDomain.EllipticCurve.parseString(i).toRight(s"unsupported curve $i"))
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
      } yield castorDomain.PublicKeyJwk.ECPublicKeyData(crv = crv, x = x, y = y)
    }
  }

  extension (template: CreateManagedDidRequestDocumentTemplate) {
    def toDomain: Either[String, walletDomain.ManagedDIDTemplate] = {
      for {
        services <- template.services.traverse(_.toDomain)
        publicKeys <- template.publicKeys.traverse(_.toDomain)
      } yield walletDomain.ManagedDIDTemplate(
        storage = template.storage,
        publicKeys = publicKeys,
        services = services
      )
    }
  }

  extension (publicKeyTemplate: CreateManagedDidRequestDocumentTemplatePublicKeysInner) {
    def toDomain: Either[String, walletDomain.DIDPublicKeyTemplate] = {
      for {
        purpose <- castorDomain.VerificationRelationship
          .parseString(publicKeyTemplate.purpose)
          .toRight(s"unsupported verificationRelationship ${publicKeyTemplate.purpose}")
      } yield walletDomain.DIDPublicKeyTemplate(
        id = publicKeyTemplate.id,
        purpose = purpose
      )
    }
  }

  extension (outcome: castorDomain.PublishedDIDOperationOutcome) {
    def toOAS: DIDOperationResponse = DIDOperationResponse(
      scheduledOperation = DidOperationSubmission(
        id = outcome.operationId.toString,
        didRef = outcome.did.toString
      )
    )
  }

  extension (domain: polluxdomain.IssueCredentialRecord) {
    def toOAS: IssueCredentialRecord = IssueCredentialRecord(
      recordId = domain.id,
      createdAt = domain.createdAt.atOffset(ZoneOffset.UTC),
      updatedAt = domain.updatedAt.map(_.atOffset(ZoneOffset.UTC)),
      role = domain.role.toString,
      subjectId = domain.subjectId,
      claims = domain.offerCredentialData
        .map(offer => offer.body.credential_preview.attributes.map(attr => (attr.name -> attr.value)).toMap)
        .getOrElse(Map.empty),
      schemaId = domain.schemaId,
      validityPeriod = domain.validityPeriod,
      automaticIssuance = domain.automaticIssuance,
      awaitConfirmation = domain.awaitConfirmation,
      protocolState = domain.protocolState.toString(),
      publicationState = domain.publicationState.map(_.toString),
      jwtCredential = domain.issueCredentialData.flatMap(issueCredential => {
        issueCredential.attachments.collectFirst { case AttachmentDescriptor(_, _, Base64(jwt), _, _, _, _) =>
          jwt
        }
      })
    )
  }
}
