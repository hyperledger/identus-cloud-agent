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

  private[anoncreds] val gvtSchemaName = "gvt2"
  private[anoncreds] val schemaVersion = "1.3"
  private[anoncreds] val credentialId1 = "id1"
  private[anoncreds] val credentialId2 = "id2"
  private[anoncreds] val credentialIdX = "idX"
  private[anoncreds] val issuerDid = "mock:issuer_id/path&q=bar"
  private[anoncreds] val attrs = Array("name", "age")
  private[anoncreds] val tails_path = Files.createTempDirectory("tails")

  "The POC script" should "run to completion" in {
    script()
  }

  def script(): Unit = {
    println(s"Version of anoncreds library is ${AnonCredsAPI.version}")

    val schema = AnonCredsAPI.createSchema().toOption.get

    val credentialDefinition = AnonCredsAPI
      .createCredentialDefinition(
        schemaId = "mock:uri2",
        schema = schema,
        issuerDid = "mock:issuer_id/path&q=bar",
        tag = "tag",
      )
      .toOption
      .get

    println(credentialDefinition.cred_def_ptr)
    println(credentialDefinition.cred_def_pvt_ptr)
    println(credentialDefinition.key_proof_ptr)

    // println(AnonCredsAPI.getJson(credentialDefinition.cred_def_ptr.getValue()))
    // println(AnonCredsAPI.getJson(credentialDefinition.cred_def_pvt_ptr.getValue()))
    // println(AnonCredsAPI.getJson(credentialDefinition.key_proof_ptr.getValue()))

    println("*" * 100)

    val credentialOffer = AnonCredsAPI.createCredentialOffer(SCHEMA_ID, CRED_DEF_ID, credentialDefinition).toOption.get

    // println(credentialOffer.ref)
    // println(AnonCredsAPI.getJson(credentialOffer.ref.getValue()))

    println("*" * 100)

    val linkSecret = LinkSecret.create.toOption.get

    // println(linkSecret.ref)
    // println(AnonCredsAPI.getJson(linkSecret.ref.getValue()))

    val credentialRequest = AnonCredsAPI
      .createCredentialRequest(
        proverDID = null,
        credDef = credentialDefinition.toPublic,
        linkSecret = linkSecret,
        linkSecretId = "linkSecretId",
        credOffer = credentialOffer,
      )
      .toOption
      .get

    println(credentialRequest.ref)
    println(credentialRequest.meta_ref)
    println(AnonCredsAPI.getJson(credentialRequest.ref.getValue()))
    println(AnonCredsAPI.getJson(credentialRequest.meta_ref.getValue()))

    // val cred_req_ptr = new PointerByReference()
    // val cred_req_meta_ptr = new PointerByReference()

    // printIfError(
    //   api.anoncreds_create_credential_request(
    //     prover_did = null,
    //     cred_def_ptr.getValue,
    //     master_secret_ptr.getValue,
    //     master_secret_id,
    //     cred_offer_ptr.getValue,
    //     cred_req_ptr,
    //     cred_req_meta_ptr
    //   )
    // )

    // print("cred_req_meta_p: ")
    // api.getJson(cred_req_meta_ptr.getValue).map(println)

    // print("cred_req_p: ")
    // api.getJson(cred_req_ptr.getValue).map(println)

    // val attr_raw_values = Array("Alan", "29")

    // val attr_enc_values_ptr = new PointerByReference()

    // printIfError(
    //   api.shim_anoncreds_encode_credential_attributes(
    //     attr_raw_values,
    //     attr_raw_values.length,
    //     attr_enc_values_ptr
    //   )
    // )

    // val attr_enc_values = attr_enc_values_ptr.getValue.getString(0).split(",")

    // val reg_def_ptr = new PointerByReference()
    // val reg_def_private_ptr = new PointerByReference()

    // printIfError(
    //   api.anoncreds_create_revocation_registry_def(
    //     cred_def_ptr.getValue,
    //     CRED_DEF_ID,
    //     issuerDid,
    //     "tag",
    //     "CL_ACCUM",
    //     2,
    //     tails_path.toString,
    //     reg_def_ptr,
    //     reg_def_private_ptr
    //   )
    // )

    // print("cred_def_p: ")
    // api.getJson(cred_def_ptr.getValue).map(println)

    // print("cred_def_pvt_p: ")
    // api.getJson(cred_def_pvt_ptr.getValue).map(println)

    // print("cred_offer: ")
    // api.getJson(cred_offer_ptr.getValue).map(println)

    // print("reg_def_p: ")
    // api.getJson(reg_def_ptr.getValue).map(println)

    // val cred_offer_json = getJsonUnsafe(reg_def_ptr.getValue)
    // println(s"reg_def_private_p: ${cred_offer_json}")

    // val pathToTailsFileIncName: String = {
    //   // the JSON lib I used failed to parse this first time.
    //   val begin = cred_offer_json.indexOf("\"tailsLocation\":") + 17
    //   val end = cred_offer_json.substring(begin).indexOf('"')
    //   cred_offer_json.substring(begin, begin + end)
    // }

    // println(s"Tails location $pathToTailsFileIncName")

    // val rev_status_list_ptr = new PointerByReference()

    // val rev_reg_def_id = "mock:uri2"

    // val timeStamp = 12L
    // printIfError(
    //   api.anoncreds_create_revocation_status_list(
    //     rev_reg_def_id,
    //     reg_def_ptr.getValue,
    //     timeStamp,
    //     0, /// not sure what this does, but only 0 seems to work
    //     rev_status_list_ptr
    //   )
    // )

    // print("rev_status_list: ")
    // api.getJson(rev_status_list_ptr.getValue).map(println)

    // val credential_ptr = new PointerByReference()

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
