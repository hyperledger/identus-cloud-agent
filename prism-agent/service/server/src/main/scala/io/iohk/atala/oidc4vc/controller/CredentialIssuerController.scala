package io.iohk.atala.oidc4vc.controller

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.api.http.ErrorResponse.badRequest
import io.iohk.atala.api.http.ErrorResponse.internalServerError
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.castor.core.model.did.{CanonicalPrismDID, PrismDID}
import io.iohk.atala.oidc4vc.CredentialIssuerEndpoints.ExtendedErrorResponse
import io.iohk.atala.oidc4vc.http.*
import io.iohk.atala.oidc4vc.http.CredentialErrorCode.*
import io.iohk.atala.oidc4vc.service.OIDCCredentialIssuerService
import io.iohk.atala.pollux.core.service.OIDC4VCIssuerMetadataService
import io.iohk.atala.shared.models.WalletAccessContext
import zio.{IO, URLayer, ZIO, ZLayer}

import java.net.URI
import java.net.URL
import java.util.UUID
import scala.language.implicitConversions

trait CredentialIssuerController {
  def issueCredential(
      ctx: RequestContext,
      didRef: String,
      credentialRequest: CredentialRequest
  ): IO[ExtendedErrorResponse, CredentialResponse]

  def createCredentialOffer(
      ctx: RequestContext,
      didRef: String,
      credentialOfferRequest: CredentialOfferRequest
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialOfferResponse]

  def getNonce(
      ctx: RequestContext,
      issuerId: UUID,
      request: NonceRequest
  ): ZIO[WalletAccessContext, ErrorResponse, NonceResponse]

  def createCredentialIssuer(
      ctx: RequestContext,
      request: CreateCredentialIssuerRequest
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialIssuer]

  def createCredentialConfiguration(
      ctx: RequestContext,
      issuerId: UUID,
      request: CreateCredentialConfigurationRequest
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialConfiguration]

  def getIssuerMetadata(
      ctx: RequestContext,
      issuerId: UUID
  ): IO[ErrorResponse, IssuerMetadata]
}

object CredentialIssuerController {
  object Errors {
    def badRequestInvalidDID(didRef: String, details: String): ExtendedErrorResponse =
      Right(
        CredentialErrorResponse(
          error = invalid_request,
          errorDescription = Some(s"Invalid DID input: $didRef. Error: $details")
        )
      )

    def badRequestDIDResolutionFailed(didRef: String, details: String): ExtendedErrorResponse =
      Right(
        CredentialErrorResponse(
          error = invalid_request,
          errorDescription = Some(s"Failed to resolve DID: $didRef. Error: $details")
        )
      )

    def badRequestInvalidProof(jwt: String, details: String): ExtendedErrorResponse =
      Right(
        CredentialErrorResponse(error = invalid_proof, errorDescription = Some(s"Invalid proof: $jwt. Error: $details"))
      )

    def badRequestUnsupportedCredentialFormat(format: CredentialFormat): ExtendedErrorResponse =
      Right(
        CredentialErrorResponse(
          error = unsupported_credential_format,
          errorDescription = Some(s"Unsupported credential format: $format")
        )
      )

    def badRequestUnsupportedCredentialType(details: String): ExtendedErrorResponse =
      Right(
        CredentialErrorResponse(
          error = unsupported_credential_type,
          errorDescription = Some(s"Unsupported credential type. Error: $details")
        )
      )

    def serverError(details: Option[String]): ExtendedErrorResponse =
      Left(internalServerError("InternalServerError", details))
  }
}

case class CredentialIssuerControllerImpl(
    credentialIssuerService: OIDCCredentialIssuerService,
    issuerMetadataService: OIDC4VCIssuerMetadataService,
    agentBaseUrl: URL
) extends CredentialIssuerController {

  import CredentialIssuerController.Errors.*
  import OIDCCredentialIssuerService.Errors.*

  private def parseIssuerDID[E](didRef: String, errorFn: (String, String) => E): IO[E, CanonicalPrismDID] = {
    for {
      prismDID <- ZIO
        .fromEither(PrismDID.fromString(didRef))
        .mapError[E](didParsingError => errorFn(didRef, didParsingError))
    } yield prismDID.asCanonical
  }

  private def parseIssuerDIDBasicError(didRef: String): IO[ErrorResponse, CanonicalPrismDID] =
    parseIssuerDID(
      didRef,
      (didRef, detail) => ErrorResponse.badRequest(detail = Some(s"Invalid DID input $didRef. $detail"))
    )

  private def parseIssuerDIDOidc4vcError(difRef: String): IO[ExtendedErrorResponse, CanonicalPrismDID] =
    parseIssuerDID(difRef, badRequestInvalidDID)

  def issueCredential(
      ctx: RequestContext,
      didRef: String,
      credentialRequest: CredentialRequest
  ): IO[ExtendedErrorResponse, CredentialResponse] = {
    credentialRequest match
      case JwtCredentialRequest(
            format,
            proof,
            credentialIdentifier,
            credentialResponseEncryption,
            credentialDefinition
          ) =>
        issueJwtCredential(didRef, proof, credentialIdentifier, credentialDefinition, credentialResponseEncryption)
      case other: CredentialRequest => // add other formats here
        ZIO.fail(badRequestUnsupportedCredentialFormat(credentialRequest.format))
  }

  def issueJwtCredential(
      didRef: String,
      maybeProof: Option[Proof],
      maybeCredentialIdentifier: Option[String],
      maybeCredentialDefinition: Option[CredentialDefinition],
      maybeEncryption: Option[CredentialResponseEncryption]
  ): IO[ExtendedErrorResponse, CredentialResponse] = {
    maybeProof match {
      case Some(JwtProof(proofType, jwt)) =>
        for {
          canonicalPrismDID: CanonicalPrismDID <- parseIssuerDIDOidc4vcError(didRef)
          _ <- ZIO
            .ifZIO(credentialIssuerService.verifyJwtProof(jwt))(
              ZIO.unit,
              ZIO.fail(OIDCCredentialIssuerService.Errors.InvalidProof("Invalid proof"))
            )
            .mapError { case InvalidProof(message) =>
              badRequestInvalidProof(jwt, message)
            }
          credentialDefinition <- ZIO
            .fromOption(maybeCredentialDefinition)
            .mapError(_ => badRequestUnsupportedCredentialType("No credential definition provided"))
          validatedCredentialDefinition <- credentialIssuerService
            .validateCredentialDefinition(credentialDefinition)
            .mapError(ue =>
              serverError(Some(s"Unexpected error while validating credential definition: ${ue.message}"))
            )
          credential <- credentialIssuerService
            .issueJwtCredential(
              canonicalPrismDID,
              maybeCredentialIdentifier,
              validatedCredentialDefinition
            )
            .mapError(ue => serverError(Some(s"Unexpected error while issuing credential: ${ue.message}")))
        } yield ImmediateCredentialResponse(credential.value)
      case None => ZIO.fail(badRequestInvalidProof(jwt = "empty", details = "No proof provided"))
    }
  }

  override def createCredentialOffer(
      ctx: RequestContext,
      didRef: String,
      credentialOfferRequest: CredentialOfferRequest
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialOfferResponse] = {
    for {
      canonicalPrismDID <- parseIssuerDIDBasicError(didRef)
      resp <- credentialIssuerService
        .createCredentialOffer(canonicalPrismDID, credentialOfferRequest.claims)
        .map(offer => CredentialOfferResponse(offer.offerUri))
        .mapError(ue =>
          internalServerError(detail = Some(s"Unexpected error while creating credential offer: ${ue.message}"))
        )
    } yield resp
  }

  override def getNonce(
      ctx: RequestContext,
      issuerId: UUID,
      request: NonceRequest
  ): ZIO[WalletAccessContext, ErrorResponse, NonceResponse] = {
    credentialIssuerService
      .getIssuanceSessionNonce(request.issuerState)
      .map(nonce => NonceResponse(nonce))
      .mapError(ue =>
        internalServerError(detail = Some(s"Unexpected error while creating credential offer: ${ue.message}"))
      )
  }

  override def createCredentialIssuer(
      ctx: RequestContext,
      request: CreateCredentialIssuerRequest
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialIssuer] = {
    for {
      authServerUrl <- ZIO
        .attempt(URI.create(request.authorizationServer).toURL())
        .mapError(ue => badRequest(detail = Some(s"Invalid URL: ${request.authorizationServer}")))
      issuer <- issuerMetadataService.createCredentialIssuer(authServerUrl)
    } yield issuer
  }

  override def createCredentialConfiguration(
      ctx: RequestContext,
      issuerId: UUID,
      request: CreateCredentialConfigurationRequest
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialConfiguration] = {
    for {
      credentialConfiguration <- issuerMetadataService.createCredentialConfiguration(
        issuerId,
        request.configurationId,
        request.schemaId
      )
    } yield credentialConfiguration: CredentialConfiguration
  }

  override def getIssuerMetadata(ctx: RequestContext, issuerId: UUID): IO[ErrorResponse, IssuerMetadata] = {
    for credentialIssuer <- issuerMetadataService.getCredentialIssuer(issuerId)
    yield IssuerMetadata.fromIssuer(credentialIssuer, agentBaseUrl)
  }
}

object CredentialIssuerControllerImpl {
  val layer
      : URLayer[AppConfig & OIDCCredentialIssuerService & OIDC4VCIssuerMetadataService, CredentialIssuerController] =
    ZLayer.fromZIO(
      for {
        agentBaseUrl <- ZIO.serviceWith[AppConfig](_.agent.httpEndpoint.publicEndpointUrl)
        oidcIssuerService <- ZIO.service[OIDCCredentialIssuerService]
        oidcIssuerMetadataService <- ZIO.service[OIDC4VCIssuerMetadataService]
      } yield CredentialIssuerControllerImpl(oidcIssuerService, oidcIssuerMetadataService, agentBaseUrl)
    )
}
