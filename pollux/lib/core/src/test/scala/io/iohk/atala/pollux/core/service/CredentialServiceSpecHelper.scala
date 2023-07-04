package io.iohk.atala.pollux.core.service

import io.circe.Json
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId}
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError.*
import io.iohk.atala.pollux.core.model.presentation.{ClaimFormat, Ldp, Options, PresentationDefinition}
import io.iohk.atala.pollux.core.repository.CredentialRepositoryInMemory
import io.iohk.atala.pollux.vc.jwt.*
import zio.*

import java.util.UUID

trait CredentialServiceSpecHelper {

  protected val irisStubLayer = ZLayer.fromZIO(
    ZIO.succeed(IrisServiceGrpc.stub(ManagedChannelBuilder.forAddress("localhost", 9999).usePlaintext.build))
  )
  protected val didResolverLayer = ZLayer.fromZIO(ZIO.succeed(makeResolver(Map.empty)))
  protected val credentialServiceLayer =
    irisStubLayer ++ CredentialRepositoryInMemory.layer ++ didResolverLayer ++ ResourceURIDereferencerImpl.layer >>> CredentialServiceImpl.layer

  protected def offerCredential(
      thid: Option[UUID] = Some(UUID.randomUUID())
  ) = OfferCredential(
    from = DidId("did:prism:issuer"),
    to = DidId("did:prism:holder"),
    thid = thid.map(_.toString),
    attachments = Seq(
      AttachmentDescriptor.buildJsonAttachment(
        payload = CredentialOfferAttachment(
          Options(UUID.randomUUID().toString(), "my-domain"),
          PresentationDefinition(format = Some(ClaimFormat(ldp = Some(Ldp(Seq("EcdsaSecp256k1Signature2019"))))))
        )
      )
    ),
    body = OfferCredential.Body(
      goal_code = Some("Offer Credential"),
      credential_preview = CredentialPreview(attributes = Seq(Attribute("name", "Alice")))
    )
  )

  protected def requestCredential(thid: Option[DidCommID] = Some(DidCommID())) = RequestCredential(
    from = DidId("did:prism:holder"),
    to = DidId("did:prism:issuer"),
    thid = thid.map(_.toString),
    attachments = Nil,
    body = RequestCredential.Body()
  )

  protected def issueCredential(thid: Option[DidCommID] = Some(DidCommID())) = IssueCredential(
    from = DidId("did:prism:issuer"),
    to = DidId("did:prism:holder"),
    thid = thid.map(_.toString),
    attachments = Nil,
    body = IssueCredential.Body()
  )

  protected def makeResolver(lookup: Map[String, DIDDocument]): DidResolver = (didUrl: String) => {
    lookup
      .get(didUrl)
      .fold(
        ZIO.succeed(DIDResolutionFailed(NotFound(s"DIDDocument not found for $didUrl")))
      )((didDocument: DIDDocument) => {
        ZIO.succeed(
          DIDResolutionSucceeded(
            didDocument,
            DIDDocumentMetadata()
          )
        )
      })
  }

  val defaultClaims = io.circe.parser
    .parse("""
        |{
        | "name":"Alice",
        | "address": {
        |   "street": "Street Name",
        |   "number": "12"
        | }
        |}
        |""".stripMargin)
    .getOrElse(Json.Null)

  extension (svc: CredentialService)
    def createRecord(
        pairwiseIssuerDID: DidId = DidId("did:prism:issuer"),
        pairwiseHolderDID: DidId = DidId("did:prism:holder-pairwise"),
        thid: DidCommID = DidCommID(),
        schemaId: Option[String] = None,
        claims: Json = defaultClaims,
        validityPeriod: Option[Double] = None,
        automaticIssuance: Option[Boolean] = None,
        awaitConfirmation: Option[Boolean] = None,
        issuingDID: Option[CanonicalPrismDID] = None
    ) = {
      svc.createIssueCredentialRecord(
        pairwiseIssuerDID = pairwiseIssuerDID,
        pairwiseHolderDID = pairwiseHolderDID,
        thid = thid,
        maybeSchemaId = schemaId,
        claims = claims,
        validityPeriod = validityPeriod,
        automaticIssuance = automaticIssuance,
        awaitConfirmation = awaitConfirmation,
        issuingDID = issuingDID
      )
    }

}
