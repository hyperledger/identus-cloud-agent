package io.iohk.atala.pollux.core.service

import io.circe.Json
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.agent.walletapi.memory.DIDSecretStorageInMemory
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.mercury.model.{AttachmentDescriptor, DidId}
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError.*
import io.iohk.atala.pollux.core.model.presentation.{ClaimFormat, Ldp, Options, PresentationDefinition}
import io.iohk.atala.pollux.core.repository.{CredentialDefinitionRepositoryInMemory, CredentialRepositoryInMemory}
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*

import java.util.UUID

trait CredentialServiceSpecHelper {

  protected val irisStubLayer = ZLayer.fromZIO(
    ZIO.succeed(IrisServiceGrpc.stub(ManagedChannelBuilder.forAddress("localhost", 9999).usePlaintext.build))
  )
  protected val didResolverLayer = ZLayer.fromZIO(ZIO.succeed(makeResolver(Map.empty)))

  protected val defaultWalletLayer = ZLayer.succeed(WalletAccessContext(WalletId.default))

  protected val credentialDefinitionServiceLayer =
    CredentialDefinitionRepositoryInMemory.layer ++ ResourceURIDereferencerImpl.layer >>>
      CredentialDefinitionServiceImpl.layer ++ defaultWalletLayer

  protected val credentialServiceLayer =
    irisStubLayer ++ CredentialRepositoryInMemory.layer ++ didResolverLayer ++ ResourceURIDereferencerImpl.layer ++
      DIDSecretStorageInMemory.layer >+> credentialDefinitionServiceLayer >>> CredentialServiceImpl.layer

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
        schemaId: String = "https://localhost/schemas/1234",
        claims: Json = defaultClaims,
        validityPeriod: Option[Double] = None,
        automaticIssuance: Option[Boolean] = None,
        awaitConfirmation: Option[Boolean] = None
    ) = for {
      issuingDID <- ZIO.fromEither(
        PrismDID.buildCanonicalFromSuffix(
          "did:prism:5c2576867a5544e5ad05cdc94f02c664b99ff65c28e8b62aada767244c2199fe:CoQBCoEBEkIKDm15LWlzc3Vpbmcta2V5EAJKLgoJc2VjcDI1NmsxEiECzoNferDd5RFveC3tIDzZTw03V9ZsadYPFTNVM286GyMSOwoHbWFzdGVyMBABSi4KCXNlY3AyNTZrMRIhAr7faqumrKkxD5c8Zg6te_crGNUe8ExGVyBdFM3uWXPv"
        )
      )
      record <- svc.createJWTIssueCredentialRecord(
        pairwiseIssuerDID = pairwiseIssuerDID,
        pairwiseHolderDID = pairwiseHolderDID,
        thid = thid,
        schemaId = schemaId,
        claims = claims,
        validityPeriod = validityPeriod,
        automaticIssuance = automaticIssuance,
        awaitConfirmation = awaitConfirmation,
        issuingDID = issuingDID
      )
    } yield record

}
