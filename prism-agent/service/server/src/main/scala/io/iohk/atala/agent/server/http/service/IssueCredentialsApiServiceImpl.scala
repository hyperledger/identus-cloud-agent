package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.circe.*
import io.iohk.atala.agent.openapi.api.IssueCredentialsApiService
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.marshaller.IssueCredentialsApiMarshallerImpl
import io.iohk.atala.pollux.vc.jwt.VerifiedCredentialJson.Encoders.Implicits.*
import io.iohk.atala.pollux.vc.jwt.VerifiedCredentialJson.Decoders.Implicits.*
import io.iohk.atala.pollux.vc.jwt.{
  CredentialSchema,
  CredentialStatus,
  Issuer,
  IssuerDID,
  JwtVerifiableCredential,
  RefreshService,
  W3CCredentialPayload
}
import zio.*

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}
import java.time.{Instant, OffsetDateTime, OffsetTime}
import java.util.{Base64, UUID}
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.decode
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.model.EncodedJWTCredential

// TODO: replace with actual implementation
class IssueCredentialsApiServiceImpl(service: CredentialService)(using runtime: Runtime[Any])
    extends IssueCredentialsApiService
    with AkkaZioSupport {

  private val issuer = service.createIssuer

  case class Schema(context: String, `type`: String)
  private val defaultSchemas = Vector(
    Schema("https://www.w3.org/2018/credentials/v1", "VerifiableCredential")
  )
  private val mockSchemas = Map(
    "06e126d1-fa44-4882-a243-1e326fbe21db" -> Schema(
      "https://www.w3.org/2018/credentials/examples/v1",
      "UniversityDegreeCredential"
    )
  )

  private val mockW3Credential = W3CCredential(
    id = "fsdf234t523fdf3",
    `type` = "University degree",
    issuer = "University of applied science",
    issuanceDate = "21/09/2022",
    credentialSubject = Map("id" -> "University degree for smth"),
    proof = Some(
      W3CProof(
        `type` = "proof",
        created = "21/09/2022",
        verificationMethod = "verificationMethod",
        proofPurpose = "proofPurpose",
        proofValue = "proofValue",
        domain = Some("domain")
      )
    )
  )

  private val mockCredentialResponse = CreateCredentials201Response(
    batchId = Some("1"),
    count = Some(1),
    credentials = Some(
      Seq("")
    )
  )

  private val mockW3CCredentialsPaginated = W3CCredentialsPaginated(
    data = Some(Seq(mockW3Credential)),
    offset = Some(1),
    limit = Some(1),
    count = Some(1)
  )

  private val mockW3CIssuanceBatchAction = W3CIssuanceBatchAction(
    action = Some("abc"),
    id = Some("abc"),
    status = Some("abc")
  )

  private val mockW3CIssuanceBatch = W3CIssuanceBatch(
    id = Some("asdf23fsdf"),
    count = Some(1),
    actions = Some(Seq(mockW3CIssuanceBatchAction))
  )

  private val mockW3CIssuanceBatchPaginated = W3CIssuanceBatchPaginated(
    data = Some(Seq(mockW3CIssuanceBatch)),
    offset = Some(1),
    limit = Some(1),
    count = Some(1)
  )

  private[this] def createPayload(input: W3CCredentialInput, issuer: Issuer): (UUID, W3CCredentialPayload) = {
    val now = Instant.now()
    val credentialId = UUID.randomUUID()
    val claims = input.claims.map(kv => kv._1 -> Json.fromString(kv._2))
    val schemas = defaultSchemas ++ input.schemaId.flatMap(mockSchemas.get)
    credentialId -> W3CCredentialPayload(
      `@context` = schemas.map(_.context),
      maybeId = Some(s"https://atala.io/prism/credentials/${credentialId.toString}"),
      `type` = schemas.map(_.`type`),
      issuer = issuer.did,
      issuanceDate = now,
      maybeExpirationDate = input.validityPeriod.map(sec => now.plusSeconds(sec.toLong)),
      maybeCredentialSchema = None,
      credentialSubject = claims.updated("id", Json.fromString(input.subjectId)).asJson,
      maybeCredentialStatus = None,
      maybeRefreshService = None,
      maybeEvidence = None,
      maybeTermsOfUse = None
    )
  }

  /** Code: 201, Message: Array of created verifiable credentials objects, DataType: CreateCredentials201Response
    */
  def createCredentials(createCredentialsRequest: CreateCredentialsRequest)(implicit
      toEntityMarshallerCreateCredentials201Response: ToEntityMarshaller[CreateCredentials201Response]
  ): Route =
    onZioSuccess(ZIO.unit) { _ =>
      val credentials = createCredentialsRequest.credentials.map { input =>
        val (uuid, payload) = createPayload(input, issuer)
        uuid.toString -> payload.toJwtCredentialPayload
      }
      val batchId = UUID.randomUUID().toString
      // service.createCredentials(
      //   batchId,
      //   credentials.map { case (id, jwt) => EncodedJWTCredential(batchId, id, jwt) }
      // )
      val resp = CreateCredentials201Response(
        Some(batchId),
        Some(credentials.size),
        Some(credentials.map(c => JwtVerifiableCredential.encodeJwt(c._2, issuer).jwt))
      )

      createCredentials201(resp)
    }

  /** Code: 204, Message: Credential was deleted
    */
  def deleteCredentialById(id: String): Route =
    onZioSuccess(ZIO.unit) { _ => deleteCredentialById204 }

  /** Code: 200, Message: Successful response, instance of Verifiable Credential is returned, DataType: W3CCredential
    * Code: 404, Message: Schema is not found by id
    */
  def getCredentialById(id: String)(implicit
      toEntityMarshallerW3CCredential: ToEntityMarshaller[W3CCredential]
  ): Route =
    onZioSuccess(ZIO.unit) { _ => getCredentialById200(mockW3Credential) }

  /** Code: 200, Message: Paginated response of the verifiable credentials objects, DataType: W3CCredentialsPaginated
    */
  def getCredentialsByBatchId(batchId: String, offset: Option[Int], limit: Option[Int])(implicit
      toEntityMarshallerW3CCredentialsPaginated: ToEntityMarshaller[W3CCredentialsPaginated]
  ): Route =
    onZioSuccess(ZIO.unit) { _ =>
      getCredentialsByBatchId200(mockW3CCredentialsPaginated)
    }

  /** Code: 200, Message: Returns the set of actions performed on the issuance-batch, DataType:
    * Seq[W3CIssuanceBatchAction]
    */
  def getIssuanceBatchActions(batchId: String)(implicit
      toEntityMarshallerW3CIssuanceBatchActionarray: ToEntityMarshaller[Seq[W3CIssuanceBatchAction]]
  ): Route =
    onZioSuccess(ZIO.unit) { _ => getIssuanceBatchActions200(Seq(mockW3CIssuanceBatchAction)) }

  /** Code: 200, Message: Returns the paginated list of issuance-batch objects, DataType: W3CIssuanceBatchPaginated
    */
  def getIssuanceBatches(limit: Option[Int], offset: Option[Int])(implicit
      toEntityMarshallerW3CIssuanceBatchPaginated: ToEntityMarshaller[W3CIssuanceBatchPaginated]
  ): Route =
    onZioSuccess(ZIO.unit) { _ =>
      getIssuanceBatches200(mockW3CIssuanceBatchPaginated)
    }

  /** Code: 200, Message: Returns the set of actions performed on the issuance-batch, DataType:
    * Seq[W3CIssuanceBatchAction]
    */
  def submitIssuanceBatchActions(batchId: String, w3CIssuanceBatchAction: Seq[W3CIssuanceBatchAction])(implicit
      toEntityMarshallerW3CIssuanceBatchActionarray: ToEntityMarshaller[Seq[W3CIssuanceBatchAction]]
  ): Route =
    onZioSuccess(ZIO.unit) { _ =>
      submitIssuanceBatchActions200(Seq(mockW3CIssuanceBatchAction))
    }

  /** Code: 200, Message: Credential was updated successfully, DataType: W3CCredential
    */
  def updateCredentialById(id: String, w3CCredentialInput: W3CCredentialInput)(implicit
      toEntityMarshallerW3CCredential: ToEntityMarshaller[W3CCredential]
  ): Route =
    onZioSuccess(ZIO.unit) { _ =>
      updateCredentialById200(mockW3Credential)
    }
}

object IssueCredentialsApiServiceImpl {
  val layer: URLayer[CredentialService, IssueCredentialsApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[CredentialService]
    } yield IssueCredentialsApiServiceImpl(svc)(using rt)
  }
}
