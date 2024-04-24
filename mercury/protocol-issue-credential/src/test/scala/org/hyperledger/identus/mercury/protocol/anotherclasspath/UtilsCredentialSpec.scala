package org.hyperledger.identus.mercury.protocol.anotherclasspath

import io.circe.*
import io.circe.syntax.*
import io.circe.generic.semiauto.*
import munit.*

import org.hyperledger.identus.mercury.model.{AttachmentDescriptor, DidId}
import org.hyperledger.identus.mercury.protocol.issuecredential.{
  IssueCredential,
  IssueCredentialProposeFormat,
  OfferCredential,
  ProposeCredential,
  CredentialPreview,
  RequestCredential,
  IssueCredentialIssuedFormat,
  IssueCredentialOfferFormat,
  IssueCredentialRequestFormat
}

private[this] case class TestCredentialType(a: String, b: Int, x: Long, name: String, dob: String)
private[this] object TestCredentialType {
  given Encoder[TestCredentialType] = deriveEncoder[TestCredentialType]
  given Decoder[TestCredentialType] = deriveDecoder[TestCredentialType]
}

/** testOnly org.hyperledger.identus.mercury.protocol.anotherclasspath.UtilsCredentialSpec
  */
class UtilsCredentialSpec extends ZSuite {
  val nameCredentialType = "prism/TestCredentialType"

  val credential = TestCredentialType(
    a = "a",
    b = 1,
    x = 2,
    name = "MyName",
    dob = "??"
  )

  val credentialPreview = CredentialPreview(attributes = Seq())
  val body = OfferCredential.Body(
    goal_code = Some("Offer Credential"),
    credential_preview = credentialPreview
  )
  val attachmentDescriptor = AttachmentDescriptor.buildJsonAttachment(payload = credential)

  test("IssueCredential encode and decode any type of Credential into the attachments") {

    val msg = IssueCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credentials =
          Seq(IssueCredentialIssuedFormat.Unsupported(nameCredentialType) -> credential.asJson.noSpaces.getBytes()),
      )
      .makeMessage

    val obj = IssueCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(
      obj.getCredentialFormatAndCredential.map(_._2),
      Seq(IssueCredentialIssuedFormat.Unsupported(nameCredentialType).name)
    )
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType).headOption, Some(credential))
  }

  test("OfferCredential encode and decode any type of Credential into the attachments") {

    val msg = OfferCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credential_preview = credentialPreview,
        credentials =
          Seq(IssueCredentialOfferFormat.Unsupported(nameCredentialType) -> credential.asJson.noSpaces.getBytes()),
      )
      .makeMessage

    val obj = OfferCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(
      obj.getCredentialFormatAndCredential.map(_._2),
      Seq(IssueCredentialOfferFormat.Unsupported(nameCredentialType).name)
    )
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType).headOption, Some(credential))
  }

  test("ProposeCredential encode and decode any type of Credential into the attachments") {

    val msg = ProposeCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credential_preview = Some(credentialPreview),
        credentials =
          Seq(IssueCredentialProposeFormat.Unsupported(nameCredentialType) -> credential.asJson.noSpaces.getBytes()),
      )
      .makeMessage

    val obj = ProposeCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(
      obj.getCredentialFormatAndCredential.map(_._2),
      Seq(IssueCredentialProposeFormat.Unsupported(nameCredentialType).name)
    )
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType).headOption, Some(credential))
  }

  test("RequestCredential encode and decode any type of Credential into the attachments") {

    val msg = RequestCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credentials =
          Seq(IssueCredentialRequestFormat.Unsupported(nameCredentialType) -> credential.asJson.noSpaces.getBytes()),
      )
      .makeMessage

    val obj = RequestCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(
      obj.getCredentialFormatAndCredential.map(_._2),
      Seq(IssueCredentialRequestFormat.Unsupported(nameCredentialType).name)
    )
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType).headOption, Some(credential))
  }
}
