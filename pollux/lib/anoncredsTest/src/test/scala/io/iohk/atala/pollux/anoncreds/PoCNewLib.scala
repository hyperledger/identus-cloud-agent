package io.iohk.atala.pollux.anoncreds

import buildinfo.BuildInfo
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.Files
import scala.jdk.CollectionConverters.*

import uniffi.anoncreds._

/** polluxAnoncredsTest/Test/testOnly io.iohk.atala.pollux.anoncreds.PoCNewLib
  */
class PoCNewLib extends AnyFlatSpec {

  val SCHEMA_ID = "mock:uri2"
  val CRED_DEF_ID = "mock:uri2"
  val ISSUER_DID = "mock:issuer_id/path&q=bar"

  "The POC New Lib script" should "run to completion" in {
    script()
  }

  def issuer_createCredDefinition = {

    // Schema {
    //  string name;
    //  string version;
    //  AttributeNames attr_names;
    //  IssuerId issuer_id;
    // }
    val schema = Schema.apply(
      SCHEMA_ID,
      "0.1.0",
      List("a", "b", "c").asJava,
      ISSUER_DID
    )

    // string schema_id,
    // Schema schema,
    // string issuer_id,
    // string tag,
    // SignatureType signature_type,
    // CredentialDefinitionConfig config
    val credentialDefinition = Issuer().createCredentialDefinition(
      SCHEMA_ID,
      schema,
      ISSUER_DID,
      "tag",
      SignatureType.CL,
      CredentialDefinitionConfig(false)
    )

    println(credentialDefinition)

    println(credentialDefinition.getCredentialDefinition().getJson())
    println(credentialDefinition.getCredentialDefinitionPrivate().getJson())
    println(credentialDefinition.getCredentialKeyCorrectnessProof().getJson())

    credentialDefinition
  }

  def issuer_createOffer(credentialDefinition: uniffi.anoncreds.IssuerCreateCredentialDefinitionReturn) = {

    // string schema_id,
    // string cred_def_id,
    // CredentialKeyCorrectnessProof correctness_proof
    val tmp = Issuer().createCredentialOffer(
      SCHEMA_ID,
      CRED_DEF_ID,
      credentialDefinition.getCredentialKeyCorrectnessProof()
    )

    println("CreateOffer")
    println(tmp.getJson())
    tmp
  }

  def holder_createCredentialRequest(
      linkSecret: LinkSecret,
      credentialDefinition: CredentialDefinition,
      credentialOffer: CredentialOffer
  ) = {

    val credentialRequest = Prover().createCredentialRequest(
      "entropy", // string? entropy,
      null, // string? prover_did,
      credentialDefinition, // CredentialDefinition cred_def,
      linkSecret, // LinkSecret link_secret,
      "linkSecretId", // string link_secret_id,
      credentialOffer, // CredentialOffer credential_offer
    )

    println("Request: ")
    println(credentialRequest.getRequest().getJson())
    println("Metadata: ")
    println(credentialRequest.getMetadata()) // FIXME Do we also need a getJson method?
    credentialRequest
  }

  def issuer_createCredential(
      credentialDefinition: CredentialDefinition,
      credentialDefinitionPrivate: CredentialDefinitionPrivate,
      credentialOffer: CredentialOffer,
      credentialRequest: CredentialRequest,
      //  java.util.List[AttributeValues] : java.util.List[AttributeValues]
      //  revocationRegistryId : String
      //  revocationStatusList : RevocationStatusList
      //  credentialRevocationConfig : CredentialRevocationConfig
  ) = {

    // CredentialDefinition cred_def,
    // CredentialDefinitionPrivate cred_def_private,
    // CredentialOffer cred_offer,
    // CredentialRequest cred_request,
    // sequence<AttributeValues> cred_values,
    // RevocationRegistryId? rev_reg_id,
    // RevocationStatusList? rev_status_list,
    // CredentialRevocationConfig? revocation_config
    val cred = Issuer().createCredential(
      credentialDefinition,
      credentialDefinitionPrivate,
      credentialOffer,
      credentialRequest,
      Seq(AttributeValues("a", "1")).asJava,
      null,
      null,
      null,
    )

    println("issuer_createCredential:")
    println(cred.getJson())
    cred
  }

  def script(): Unit = {
    println(s"Version of anoncreds library")

    println("*** issuer " + ("*" * 100))
    val credentialDefinition = issuer_createCredDefinition

    val credentialOffer = issuer_createOffer(credentialDefinition)

    println("*** holder " + ("*" * 100))

    val linkSecret = LinkSecret()
    println(linkSecret.getBigNumber()) // TODO REMOVE
    println(
      linkSecret.getJson()
    ) // FIXME this printing 5FCC7E4BC91B265C59E059369E5C49901AB38E50DF20C218F7B2135E165BAE3C and should have '"' like: "5FCC7E4BC91B265C59E059369E5C49901AB38E50DF20C218F7B2135E165BAE3C"

    val credentialRequest =
      holder_createCredentialRequest(linkSecret, credentialDefinition.getCredentialDefinition(), credentialOffer)

    println("*" * 100)

    issuer_createCredential(
      credentialDefinition.getCredentialDefinition(),
      credentialDefinition.getCredentialDefinitionPrivate(),
      credentialOffer,
      credentialRequest.getRequest()
    )
  }

}
