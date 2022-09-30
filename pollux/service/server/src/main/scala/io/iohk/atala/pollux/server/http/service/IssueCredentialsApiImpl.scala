package io.iohk.atala.pollux.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.pollux.openapi.api.IssueCredentialsApiService
import io.iohk.atala.pollux.openapi.model.*
import io.iohk.atala.pollux.server.http.marshaller.IssueCredentialsApiMarshallerImpl
import zio.*

// TODO: replace with actual implementation
class IssueCredentialsApiImpl()(using runtime: Runtime[Any]) extends IssueCredentialsApiService with AkkaZioSupport {

  private val mockW3Credential = W3CCredential(
    id = "fsdf234t523fdf3",
    `type` = "University degree",
    issuer = "University of applied science",
    issuanceDate = "21/09/2022",
    credentialSubject = Some(
      W3CCredentialCredentialSubject(
        id = "University degree for smth"
      )
    ),
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
      Seq(mockW3Credential)
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

  /** Code: 201, Message: Array of created verifiable credentials objects, DataType: CreateCredentials201Response
    */
  def createCredentials(createCredentialsRequest: CreateCredentialsRequest)(implicit
      toEntityMarshallerCreateCredentials201Response: ToEntityMarshaller[CreateCredentials201Response]
  ): Route =
    onZioSuccess(ZIO.unit) { _ => createCredentials201(mockCredentialResponse) }

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

object IssueCredentialsApiImpl {
  val layer: ZLayer[Any, Nothing, IssueCredentialsApiImpl] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
    } yield IssueCredentialsApiImpl()(using rt)
  }
}
