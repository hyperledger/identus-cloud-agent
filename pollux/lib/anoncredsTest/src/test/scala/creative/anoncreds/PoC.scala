package creative.anoncreds

import buildinfo.BuildInfo
import creative.anoncreds.AnonCredsOps.{FfiCredentialEntry, FfiCredentialProve, Helpers}
import creative.anoncreds.PrintErrorCode.printIfError
import jnr.ffi.{Pointer, TypeAlias}
import jnr.ffi.byref.{ByteByReference, NumberByReference, PointerByReference}
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.Files
import io.iohk.atala.pollux.anoncreds._

/** polluxAnoncredsTest/Test/testOnly creative.anoncreds.PoC
  */
class PoC extends AnyFlatSpec {

  val SCHEMA_ID = "mock:uri2"
  val CRED_DEF_ID = "mock:uri2"
  val TAG = Tag("tag")
  val ISSUER_DID = "mock:issuer_id/path&q=bar"

  private[anoncreds] val gvtSchemaName = "gvt2"
  private[anoncreds] val schemaVersion = "1.3"
  private[anoncreds] val credentialId1 = "id1"
  private[anoncreds] val credentialId2 = "id2"
  private[anoncreds] val credentialIdX = "idX"
  private[anoncreds] val attrs = Array("name", "age")
  private[anoncreds] val tails_path = Files.createTempDirectory("tails")

  "The POC script" should "run to completion" in {
    script()
  }

  def issuer_createCredDefinition = {
    val schema = AnonCredsAPI.createSchema().toOption.get

    val credentialDefinition = AnonCredsAPI
      .createCredentialDefinition(
        schemaId = SCHEMA_ID,
        schema = schema,
        issuerDid = ISSUER_DID,
        tag = TAG,
      )
      .toOption
      .get

    // println(credentialDefinition.pub)
    // println(credentialDefinition.pvt)
    // println(credentialDefinition.keyProof)
    // println(AnonCredsAPI.getJson(credentialDefinition.cred_def_ptr.getValue()))
    // println(AnonCredsAPI.getJson(credentialDefinition.cred_def_pvt_ptr.getValue()))
    // println(AnonCredsAPI.getJson(credentialDefinition.key_proof_ptr.getValue()))
    credentialDefinition
  }

  def issuer_createOffer(credentialDefinition: CredentialDefinitionPrivate) = {
    val tmp = AnonCredsAPI.createCredentialOffer(SCHEMA_ID, CRED_DEF_ID, credentialDefinition).toOption.get
    // println("CreateOffer")
    // println(tmp.ref)
    // println(AnonCredsAPI.getJson(tmp.ref.getValue()))
    tmp
  }

  def holder_createCredentialRequest(
      linkSecret: LinkSecret,
      credentialDefinition: CredentialDefinitionPublic,
      credentialOffer: CredentialOffer
  ) = {
    val credentialRequest = AnonCredsAPI
      .createCredentialRequest(
        proverDID = null,
        credDef = credentialDefinition,
        linkSecret = linkSecret,
        linkSecretId = "linkSecretId",
        credOffer = credentialOffer,
      )
      .toOption
      .get

    // print("cred_req_p: ")
    // println(credentialRequest.ref)
    // println(AnonCredsAPI.getJson(credentialRequest.ref.getValue()))
    // print("cred_req_meta_p: ")
    // println(credentialRequest.meta_ref)
    // println(AnonCredsAPI.getJson(credentialRequest.meta_ref.getValue()))
    credentialRequest
  }

  def issuer_createRevocationRegistryDefinition(credentialDefinition: CredentialDefinitionPrivate) = {
    val revocationRegistry = AnonCredsAPI
      .createRevocationRegistry(
        credentialDefinition = credentialDefinition,
        credDefId = CRED_DEF_ID,
        issuerDID = ISSUER_DID,
        tag = TAG,
      )
      .toOption
      .get

    // println("cred_def_p: ")
    // AnonCredsAPI.getJson(credentialDefinition.pub.getValue).map(println)
    // println("cred_def_pvt_p: ")
    // AnonCredsAPI.getJson(credentialDefinition.pvt.getValue).map(println)
    // // println("cred_offer: ")
    // // api.getJson(cred_offer_ptr.getValue).map(println)
    // println("reg_def_p: ")
    // AnonCredsAPI.getJson(revocationRegistry.pub.getValue).map(println)
    // println("reg_def_private_p: ")
    // AnonCredsAPI.getJson(revocationRegistry.pvt.getValue).map(println)

    revocationRegistry
  }

  def script(): Unit = {
    println(s"Version of anoncreds library is ${AnonCredsAPI.version}")

    println("*** issuer " + ("*" * 100))
    val credentialDefinition = issuer_createCredDefinition

    val credentialOffer = issuer_createOffer(credentialDefinition)

    println("*** holder " + ("*" * 100))

    val linkSecret = LinkSecret.create.toOption.get
    // println(linkSecret.ref)
    // println(AnonCredsAPI.getJson(linkSecret.ref.getValue()))

    val credentialRequest = holder_createCredentialRequest(linkSecret, credentialDefinition.toPublic, credentialOffer)

    println("*" * 100)

    val attrsValue = Array("Alan", "29")
    val encodeCredentialAttributes = AnonCredsAPI.encodeCredentialAttributes(attrsValue).toOption.get

    val attr_enc_values = encodeCredentialAttributes.ref.getValue.getString(0) // .split(",")
    println(attr_enc_values.toString)

    println("*" * 100)

    val revocationRegistryDefinition = issuer_createRevocationRegistryDefinition(credentialDefinition)

    // val pathToTailsFileIncName: String = {
    //   // the JSON lib I used failed to parse this first time.
    //   val begin = cred_offer_json.indexOf("\"tailsLocation\":") + 17
    //   val end = cred_offer_json.substring(begin).indexOf('"')
    //   cred_offer_json.substring(begin, begin + end)
    // }
    // println(s"Tails location $pathToTailsFileIncName")

    val revocationStatusList = AnonCredsAPI
      .createRevocationStatusList(
        revRegDefId = "mock:uri2",
        revocationRegistryDefinition = revocationRegistryDefinition
      )
      .toOption
      .get

    print("rev_status_list: ")
    AnonCredsAPI.getJson(revocationStatusList.ref.getValue).map(println)

    println("*" * 100)

    val credential = AnonCredsAPI
      .createCredential(
        credDef = credentialDefinition,
        credOffer = credentialOffer,
        credRequest = credentialRequest,
        attrNames = attrs,
        attrRawValues = attrsValue,
        attrEncValues = encodeCredentialAttributes,
        revocationRegistryDefinition = revocationRegistryDefinition,
        revocationStatusList = revocationStatusList
      )
    // .toOption
    // .get

    println(credential)

    // printIfError(
    //   api.shim_anoncreds_create_credential(
    //     cred_def_ptr.getValue,
    //     cred_def_pvt_ptr.getValue,
    //     cred_offer_ptr.getValue,
    //     cred_req_ptr.getValue,
    //     attrs,
    //     attrs.length,
    //     attr_raw_values,
    //     attr_raw_values.length,
    //     attr_enc_values,
    //     attr_enc_values.length,
    //     rev_reg_def_id,
    //     rev_status_list_ptr.getValue,
    //     reg_def_ptr.getValue,
    //     reg_def_private_ptr.getValue,
    //     0L,
    //     pathToTailsFileIncName,
    //     credential_ptr
    //   )
    // )

    // printObjectHandleType(cred_req_meta_ptr.getValue)
    // printObjectHandleType(master_secret_ptr.getValue)
    // printObjectHandleType(cred_def_ptr.getValue)
    // printObjectHandleType(reg_def_ptr.getValue)
    // printObjectHandleType(credential_ptr.getValue)

    // val processed_credential_ptr = new PointerByReference()
    // println("Processing Credential...")
    // printIfError(
    //   api.anoncreds_process_credential(
    //     credential_ptr.getValue,
    //     cred_req_meta_ptr.getValue,
    //     master_secret_ptr.getValue,
    //     cred_def_ptr.getValue,
    //     reg_def_ptr.getValue,
    //     processed_credential_ptr
    //   )
    // )

    // printObjectHandleType(processed_credential_ptr.getValue)
    // println(s"processed_credential: ${getJsonUnsafe(processed_credential_ptr.getValue)}")

    // val newNonce = getNonceUnsafe

    // val referant = "issuerDid"

    // val pres_req = s"""{
    //   "nonce": "$newNonce",
    //   "name": "name",
    //   "version": "1.0",
    //   "requested_attributes": { "$referant":{"name":"name"}, "name":{"name":"name"}  },
    //   "requested_predicates": {}
    // }"""

    // val presentation_req_ptr = new PointerByReference()
    // printIfError(
    //   api.shim_anoncreds_presentation_request_from_json(pres_req, pres_req.length, presentation_req_ptr)
    // )

    // printObjectHandleType(presentation_req_ptr.getValue)
    // println(s"presentation_req: ${getJsonUnsafe(presentation_req_ptr.getValue)}")

    // val revocationStatePtr = new PointerByReference()

    // printIfError(
    //   api.anoncreds_create_or_update_revocation_state(
    //     reg_def_ptr.getValue,
    //     rev_status_list_ptr.getValue,
    //     0L,
    //     pathToTailsFileIncName,
    //     Pointer.newIntPointer(api.runtime, 0L),
    //     rev_status_list_ptr.getValue,
    //     revocationStatePtr
    //   )
    // )

    // print("revocationStatePtr ")
    // printObjectHandleType(revocationStatePtr.getValue)

    // val credentials = Seq(
    //   FfiCredentialEntry(
    //     processed_credential_ptr.getValue,
    //     timeStamp,
    //     revocationStatePtr.getValue,
    //   )
    // )

    // val credentialProve = Seq(
    //   FfiCredentialProve(
    //     0L,
    //     referant,
    //     0,
    //     0
    //   ),
    //   FfiCredentialProve(
    //     0L,
    //     "name",
    //     0,
    //     0
    //   )
    // )

    // val selfAttestNames = Seq("name")
    // val selfAttestValues = Seq("Alan")

    // val schemas = Seq(schema)
    // val schemaIds = Seq(SCHEMA_ID)
    // val credDefinitions = Seq(cred_def_ptr.getValue)
    // val credDefinitionIds = Seq(CRED_DEF_ID)

    // val presentationPtr = new PointerByReference()

    // printIfError(
    //   api.createPresentation(
    //     presentation_req_ptr.getValue,
    //     credentials,
    //     credentialProve,
    //     selfAttestNames,
    //     selfAttestValues,
    //     master_secret_ptr.getValue,
    //     schemas,
    //     schemaIds,
    //     credDefinitions,
    //     credDefinitionIds,
    //     presentationPtr
    //   )
    // )

    // print(s"createPresentation ")
    // printObjectHandleType(presentationPtr.getValue)

    // val presentationVerification = new StringBuffer(100)

    // val regDefinitions = Seq(reg_def_ptr.getValue)
    // val regDefinitionIds = Seq(rev_reg_def_id)
    // val revStatusList = Seq(rev_status_list_ptr.getValue)

    // printIfError(
    //   api.verifyPresentation(
    //     presentationPtr.getValue,
    //     presentation_req_ptr.getValue,
    //     schemas,
    //     schemaIds,
    //     credDefinitions,
    //     credDefinitionIds,
    //     regDefinitions,
    //     regDefinitionIds,
    //     revStatusList,
    //     presentationVerification
    //   )
    // )

    // // TODO what does 0 mean? I believe it's "truthy" as per js wrapper tests,
    // // but in any event - no errors in the script.
    // // next level up is - what does all this mean?
    // println(s"presentationVerification is $presentationVerification")
  }

  def printObjectHandleType(p: Pointer)(implicit api: AnonCreds): Unit = {
    val out = new PointerByReference()
    api.anoncreds_object_get_type_name(p, out)
    Option(out.getValue) match {
      case Some(value) =>
        println(s"Object Handle Type:  ${value.getString(0)}")
      case None =>
        println("NO Object found?")
    }

  }

  def getJsonUnsafe(p: Pointer)(implicit api: AnonCreds): String = {
    api.getJson(p) match {
      case Left(value) =>
        throw new RuntimeException(s"json error $value")

      case Right(value) => value
    }

  }

  def getNonceUnsafe(implicit api: AnonCreds): String = {
    api.getNonce.getOrElse(throw new RuntimeException("Can't get Nonce"))
  }
}
