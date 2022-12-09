package io.iohk.atala.agent.server.http.service

import io.iohk.atala.agent.openapi.api.PresentProofApiService
import io.iohk.atala.agent.openapi.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route

import zio._
import scala.concurrent.Future
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof._
import java.util.UUID
import io.iohk.atala.mercury.DidComm
import io.iohk.atala.agent.server.http.model.OASDomainModelHelper
import io.iohk.atala.agent.server.http.model.OASErrorModelHelper
import io.iohk.atala.agent.server.http.model.InvalidState
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.agent.server.http.model.HttpServiceError.DomainError

import io.iohk.atala.pollux.vc.jwt.Issuer
import io.iohk.atala.pollux.core.service.PresentationService
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.PresentationRecord

class PresentProofApiServiceImpl(
    presentationService: PresentationService,
    connectionService: ConnectionService,
    didCommService: DidComm
)(using runtime: Runtime[Any])
    extends PresentProofApiService
    with AkkaZioSupport
    with OASDomainModelHelper
    with OASErrorModelHelper {

  override def requestPresentation(requestPresentationInput: RequestPresentationInput)(implicit
      toEntityMarshallerRequestPresentationOutput: ToEntityMarshaller[RequestPresentationOutput],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val didRegex = "^did.*".r

    val result = for {
      didId <- requestPresentationInput.connectionId match {
        case didRegex() => ZIO.succeed(DidId(requestPresentationInput.connectionId))
        case _ =>
          connectionService
            .getConnectionRecord(UUID.fromString(requestPresentationInput.connectionId))
            .map(_.flatMap(_.connectionRequest).map(_.from).get) // TODO GET
            .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply)
            .mapError(_.toOAS)
      }

      record <- presentationService
        .createPresentationRecord(
          thid = UUID.randomUUID(),
          subjectDid = didId,
          connectionId = None,
          proofTypes = requestPresentationInput.proofs.map { e =>
            ProofType(
              schema = e.schemaId, // TODO rename field to schemaId
              requiredFields = None,
              trustIssuers = Some(e.trustIssuers.map(DidId(_)))
            )
          }
        )
        .mapError(HttpServiceError.DomainError[PresentationError].apply)
        .mapError(_.toOAS)
    } yield RequestPresentationOutput(record.id.toString)

    onZioSuccess(result.either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => requestPresentation201(result)
    }

  }

  override def getAllPresentation()(implicit
      toEntityMarshallerPresentationStatusarray: ToEntityMarshaller[Seq[PresentationStatus]],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {

    val result = for {
      records <- presentationService
        .getPresentationRecords()
        .mapError(HttpServiceError.DomainError[PresentationError].apply)

      presentationStatus = records.map { record =>
        val connectionId = None // record.subjectId // TODO
        PresentationStatus(record.id.toString, record.protocolState.toString, Seq.empty, connectionId)
      }
    } yield presentationStatus

    onZioSuccess(result.mapError(_.toOAS).either) {
      case Left(error) => complete(error.status -> error)
      case Right(results) => {

        getAllPresentation200(results)
      }
    }
  }

  override def updatePresentation(id: String, requestPresentationAction: RequestPresentationAction)(implicit
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = requestPresentationAction.action match {

      case "request-accept" =>
        // TODO IMPORTANT: ATL-2702
        // this function is used as a temporary replacement
        // eventually, prism-agent should use castor library to get the issuer (issuance key and did)
        def createHolder: Issuer = {
          import java.security.KeyPairGenerator
          import java.security.spec.ECGenParameterSpec
          import java.security.KeyPairGenerator
          import java.security.SecureRandom
          val keyGen = KeyPairGenerator.getInstance("EC")
          val ecSpec = ECGenParameterSpec("secp256r1")
          keyGen.initialize(ecSpec, SecureRandom())
          val keyPair = keyGen.generateKeyPair()
          val privateKey = keyPair.getPrivate
          val publicKey = keyPair.getPublic
          val uuid = UUID.randomUUID().toString
          Issuer(
            did = io.iohk.atala.pollux.vc.jwt.DID(s"did:prism:$uuid"),
            signer = io.iohk.atala.pollux.vc.jwt.ES256Signer(privateKey),
            publicKey = publicKey
          )
        }

        for {
          record <- presentationService
            .acceptRequestPresentation(
              recordId = UUID.fromString(id),
              crecentialsToUse = requestPresentationAction.proofId.getOrElse(Seq.empty),
              prover = createHolder
            )
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record
      case "request-reject" => {
        for {
          record <- presentationService
            .rejectRequestPresentation(UUID.fromString(id))
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record
      }
      case "presentation-accept" =>
        for {
          record <- presentationService
            .acceptPresentation(UUID.fromString(id))
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record // TODO FIXME
      case "presentation-reject" => {
        for {
          record <- presentationService
            .rejectPresentation(UUID.fromString(id))
            .mapError(HttpServiceError.DomainError[PresentationError].apply)
        } yield record
      }
      case s =>
        throw InvalidState(
          s"Error: updatePresentation's State must be 'request-accept','request-reject', 'presentation-accept' or 'presentation-reject' but is $s"
        )
    }

    onZioSuccess(result.mapBoth(_.toOAS, record => record).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => updatePresentation200
    }
  }
}

object PresentProofApiServiceImpl {
  val layer: URLayer[PresentationService & ConnectionService & DidComm, PresentProofApiService] = ZLayer.fromZIO {

    for {
      rt <- ZIO.runtime[Any]
      presentationService <- ZIO.service[PresentationService]
      connectionService <- ZIO.service[ConnectionService]
      didCommService <- ZIO.service[DidComm]
    } yield PresentProofApiServiceImpl(presentationService, connectionService, didCommService)(using rt)
  }
}
