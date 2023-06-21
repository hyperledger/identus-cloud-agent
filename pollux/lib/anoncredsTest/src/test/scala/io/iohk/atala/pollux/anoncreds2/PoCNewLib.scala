package io.iohk.atala.pollux.anoncreds2

import buildinfo.BuildInfo
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.Files
import scala.jdk.CollectionConverters.*

import uniffi.anoncreds._
import io.iohk.atala.pollux.anoncreds.AnonCredsAPI.createCredentialRequest

/** polluxAnoncredsTest/Test/testOnly io.iohk.atala.pollux.anoncreds2.PoCNewLib
  */
class PoCNewLib extends AnyFlatSpec {

  val SCHEMA_ID = "mock:uri2"
  val CRED_DEF_ID = "mock:uri2"
  // val TAG = Tag("tag")
  val ISSUER_DID = "mock:issuer_id/path&q=bar"

  // private[anoncreds] val gvtSchemaName = "gvt2"
  // private[anoncreds] val schemaVersion = "1.3"
  // private[anoncreds] val credentialId1 = "id1"
  // private[anoncreds] val credentialId2 = "id2"
  // private[anoncreds] val credentialIdX = "idX"
  // private[anoncreds] val attrs = Array("name", "age")
  // private[anoncreds] val tails_path = Files.createTempDirectory("tails")

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

    println(credentialDefinition.getCredentialDefinition())
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
    println(tmp)
    // println(AnonCredsAPI.getJson(tmp.ref.getValue()))
    tmp
  }

  def holder_createCredentialRequest(
      linkSecret: LinkSecret,
      credentialDefinition: CredentialDefinition,
      credentialOffer: CredentialOffer
  ) = {

    // string? entropy,
    // string? prover_did,
    // CredentialDefinition cred_def,
    // LinkSecret link_secret,
    // string link_secret_id,
    // CredentialOffer credential_offer

    val credentialRequest = Prover().createCredentialRequest(
      "entropy",
      null,
      credentialDefinition,
      linkSecret,
      "linkSecretId",
      credentialOffer,
    )

    print("Request: ")
    println(credentialRequest.getRequest().getJson())
    print("Metadata: ")
    println(credentialRequest.getMetadata())
    // println(AnonCredsAPI.getJson(credentialRequest.ref.getValue()))
    // print("cred_req_meta_p: ")
    // println(credentialRequest.meta_ref)
    // println(AnonCredsAPI.getJson(credentialRequest.meta_ref.getValue()))
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

    println(cred.getJson())
  }

  def script(): Unit = {
    println(s"Version of anoncreds library")

    println("*** issuer " + ("*" * 100))
    val credentialDefinition = issuer_createCredDefinition

    val credentialOffer = issuer_createOffer(credentialDefinition)

    println("*** holder " + ("*" * 100))

    val linkSecret = LinkSecret()
    println(linkSecret.getBigNumber())

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
