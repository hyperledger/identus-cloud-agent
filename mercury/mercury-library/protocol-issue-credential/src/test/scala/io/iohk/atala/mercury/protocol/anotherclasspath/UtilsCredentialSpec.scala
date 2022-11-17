package io.iohk.atala.mercury.protocol.anotherclasspath

import cats.implicits.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.generic.semiauto.*
import zio.*
import munit.*

import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.protocol.issuecredential._

private[this] case class TestCredentialType(a: String, b: Int, x: Long, name: String, dob: String)
private[this] object TestCredentialType {
  given Encoder[TestCredentialType] = deriveEncoder[TestCredentialType]
  given Decoder[TestCredentialType] = deriveDecoder[TestCredentialType]
}

/** testOnly io.iohk.atala.mercury.protocol.anotherclasspath.UtilsCredentialSpec
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
  val attachmentDescriptor = AttachmentDescriptor.buildAttachment(payload = credential)

  test("IssueCredential encode and decode any type of Credential into the attachments") {

    val msg = IssueCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credentials = Map(nameCredentialType -> credential),
      )
      .makeMessage

    val obj = IssueCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(obj.getCredentialFormatAndCredential.keySet, Set(nameCredentialType))
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType), Some(credential))
  }

  test("OfferCredential encode and decode any type of Credential into the attachments") {

    val msg = OfferCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credential_preview = credentialPreview,
        credentials = Map(nameCredentialType -> credential),
      )
      .makeMessage

    val obj = OfferCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(obj.getCredentialFormatAndCredential.keySet, Set(nameCredentialType))
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType), Some(credential))
  }

  test("ProposeCredential encode and decode any type of Credential into the attachments") {

    val msg = ProposeCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credential_preview = credentialPreview,
        credentials = Map(nameCredentialType -> credential),
      )
      .makeMessage

    val obj = ProposeCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(obj.getCredentialFormatAndCredential.keySet, Set(nameCredentialType))
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType), Some(credential))
  }

  test("RequestCredential encode and decode any type of Credential into the attachments") {

    val msg = RequestCredential
      .build(
        fromDID = DidId("did:prism:test123from"),
        toDID = DidId("did:prism:test123to"),
        credentials = Map(nameCredentialType -> credential),
      )
      .makeMessage

    val obj = RequestCredential.readFromMessage(msg)

    assertEquals(obj.getCredentialFormatAndCredential.size, 1)
    assertEquals(obj.getCredentialFormatAndCredential.keySet, Set(nameCredentialType))
    assertEquals(obj.getCredential[TestCredentialType](nameCredentialType), Some(credential))
  }
}
