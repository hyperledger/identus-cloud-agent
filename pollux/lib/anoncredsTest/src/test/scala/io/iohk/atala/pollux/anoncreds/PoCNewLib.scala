package io.iohk.atala.pollux.anoncreds

import org.scalatest.flatspec.AnyFlatSpec
import scala.jdk.CollectionConverters.*

/** polluxAnoncredsTest/Test/testOnly io.iohk.atala.pollux.anoncreds.PoCNewLib
  */
class PoCNewLib extends AnyFlatSpec {

  val SCHEMA_ID = "mock:uri2"
  val CRED_DEF_ID = "mock:uri2"
  val ISSUER_DID = "mock:issuer_id/path&q=bar"

  "The POC New Lib script" should "run to completion" in {
    script()
  }

  def script(): Unit = {
    println(s"Version of anoncreds library")

    // ##############
    // ### ISSUER ###
    // ##############
    println("*** issuer " + ("*" * 100))
    // ############################################################################################
    val schema = AnoncredLib.createSchema(
      SCHEMA_ID,
      "0.1.0",
      Set("name", "sex", "age"),
      ISSUER_DID
    )

    // ############################################################################################
    val credentialDefinition = AnoncredLib.createCredDefinition(ISSUER_DID, schema, "tag", supportRevocation = false)

    // // ############################################################################################
    val credentialOffer = AnoncredLib.createOffer(credentialDefinition, CRED_DEF_ID)

    // ##############
    // ### HOLDER ###
    // ##############
    println("*** holder " + ("*" * 100))

    val linkSecret = LinkSecretWithId("ID_of_some_secret_1")

    val credentialRequest = AnoncredLib.createCredentialRequest(linkSecret, credentialDefinition.cd, credentialOffer)
    println("*" * 100)

    val credential = AnoncredLib.createCredential(
      credentialDefinition.cd,
      credentialDefinition.cdPrivate,
      credentialOffer,
      credentialRequest.request,
      Seq(
        ("name", "Miguel"),
        ("sex", "M"),
        ("age", "31"),
      )
    )

    println(credential)
  }

}
