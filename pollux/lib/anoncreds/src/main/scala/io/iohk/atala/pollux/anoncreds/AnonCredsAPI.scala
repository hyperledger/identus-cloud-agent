package io.iohk.atala.pollux.anoncreds

import jnr.ffi.Pointer
import jnr.ffi.byref.PointerByReference
import creative.anoncreds.AnonCreds
import creative.anoncreds.ErrorCode
import java.nio.file.Files
import creative.anoncreds.AnonCredsOps
import creative.anoncreds.AnonCredsOps.{FfiCredentialEntry, FfiCredentialProve, Helpers}

trait CredentialDefinition {
  def cred_def_ptr: PointerByReference
  def key_proof_ptr: PointerByReference
}
case class CredentialDefinitionPublic(
    cred_def_ptr: PointerByReference,
    key_proof_ptr: PointerByReference,
) extends CredentialDefinition
case class CredentialDefinitionPrivate(
    cred_def_ptr: PointerByReference,
    cred_def_pvt_ptr: PointerByReference,
    key_proof_ptr: PointerByReference,
) extends CredentialDefinition {
  def toPublic = CredentialDefinitionPublic(
    cred_def_ptr: PointerByReference,
    cred_def_pvt_ptr: PointerByReference,
  )
}

case class SchemaRef(ref: PointerByReference) {
  def getPointer = ref.getValue()
  def json = AnonCredsAPI.getJson(ref.getValue)
}

enum SigType:
  case CL extends SigType
  // .... ?

extension (code: ErrorCode)
  def onSuccess[T](defualt: T) = code match
    case ErrorCode.SUCCESS                => Right(defualt)
    case ErrorCode.INPUT                  => Left("Error INPUT")
    case ErrorCode.IOERROR                => Left("Error IOERROR")
    case ErrorCode.INVALIDSTATE           => Left("Error INVALIDSTATE")
    case ErrorCode.UNEXPECTED             => Left("Error UNEXPECTED")
    case ErrorCode.CREDENTIALREVOKED      => Left("Error CREDENTIALREVOKED")
    case ErrorCode.INVALIDUSERREVOCID     => Left("Error INVALIDUSERREVOCID")
    case ErrorCode.PROOFREJECTED          => Left("Error PROOFREJECTED")
    case ErrorCode.REVOCATIONREGISTRYFULL => Left("Error REVOCATIONREGISTRYFULL")

case class LinkSecret(ref: PointerByReference = new PointerByReference())
object LinkSecret {
  def create: Either[String, LinkSecret] = {
    val linkSecret = new LinkSecret()
    AnonCredsAPI.api
      .anoncreds_create_master_secret(linkSecret.ref)
      .onSuccess(linkSecret)
  }
}
case class CredentialOffer(
    ref: PointerByReference = new PointerByReference()
)

case class CredentialRequest(
    ref: PointerByReference = new PointerByReference(),
    meta_ref: PointerByReference = new PointerByReference(),
)

case class EncodeCredentialAttributes(ref: PointerByReference = new PointerByReference()) {
  def values: Array[String] = ref.getValue.getString(0).split(",")
}
case class Credential(ref: PointerByReference = new PointerByReference())

object AnonCredsAPI {

  val api: AnonCreds = AnonCreds()
  api.anoncreds_set_default_logger()

  def version = api.anoncreds_version()

  def getJson(p: Pointer): Either[String, String] = {

    val buf = new PointerByReference()

    val data = api
      .shim_anoncreds_object_get_json(
        p,
        buf
      )
      .onSuccess(buf.getValue.getString(0))

    data

  }

  def createSchema(
      issuerDid: String = "mock:issuer_id/path&q=bar",
      attrs: Array[String] = Array("name", "age")
  ): Either[String, SchemaRef] = {

    val gvtSchemaName = "gvt2"
    val schemaVersion = "1.3"

    val tails_path = Files.createTempDirectory("tails")
    val result_p_int = new PointerByReference()

    api
      .shim_anoncreds_create_schema(
        gvtSchemaName,
        schemaVersion,
        issuerDid,
        attrs,
        attrs.length,
        result_p_int
      )
      .onSuccess(SchemaRef(result_p_int))
  }

  def createCredentialDefinition(
      schemaId: String,
      schema: SchemaRef,
      issuerDid: String,
      tag: String,
      sigType: SigType = SigType.CL,
      supportRevocation: Boolean = false,
  ): Either[String, CredentialDefinitionPrivate] = {
    val cred_def_ptr = new PointerByReference()
    val cred_def_pvt_ptr = new PointerByReference()
    val key_proof_ptr = new PointerByReference()

    // println(schemaId)
    // println(schema.getPointer)
    // println("tag")
    // println(issuerDid)
    // println(sigType.toString)
    // println(if (supportRevocation) (0: Byte) else (1: Byte))
    // println(cred_def_ptr)
    // println(cred_def_pvt_ptr)
    // println(key_proof_ptr)

    api
      .anoncreds_create_credential_definition(
        schemaId,
        schema.getPointer,
        "tag",
        issuerDid,
        sigType.toString,
        if (supportRevocation) (0: Byte) else (1: Byte),
        cred_def_ptr,
        cred_def_pvt_ptr,
        key_proof_ptr
      )
      .onSuccess(
        CredentialDefinitionPrivate(
          cred_def_ptr,
          cred_def_pvt_ptr,
          key_proof_ptr
        )
      )

  }

  def createCredentialOffer(
      schemaId: String, // FfiStr,
      credDefId: String, // FfiStr,
      // key_proof: Pointer,
      credentialDefinition: CredentialDefinitionPrivate
  ): Either[String, CredentialOffer] = {
    val offer = CredentialOffer()
    api
      .anoncreds_create_credential_offer(
        schema_id = schemaId,
        cred_def_id = credDefId,
        key_proof = credentialDefinition.key_proof_ptr.getValue(),
        cred_offer_p = offer.ref,
      )
      .onSuccess(offer)
  }

  // *********************************************************************

  def createCredentialRequest(
      proverDID: String = null,
      credDef: CredentialDefinitionPublic,
      linkSecret: LinkSecret,
      linkSecretId: String,
      credOffer: CredentialOffer,
  ): Either[String, CredentialRequest] = {
    val tmp = CredentialRequest()

    api
      .anoncreds_create_credential_request(
        proverDID,
        credDef.cred_def_ptr.getValue,
        linkSecret.ref.getValue,
        linkSecretId,
        credOffer.ref.getValue,
        tmp.ref,
        tmp.meta_ref,
      )
      .onSuccess(tmp)
  }

  def encodeCredentialAttributes(attr: Array[String]): Either[String, EncodeCredentialAttributes] = {
    val tmp = EncodeCredentialAttributes()
    api
      .shim_anoncreds_encode_credential_attributes(
        attr,
        attr.length,
        tmp.ref
      )
      .onSuccess(tmp)
  }

  def createCredential(
      credDef: CredentialDefinitionPrivate,
      credOffer: CredentialOffer,
      credRequest: CredentialRequest,
      attrNames: Array[String],
      attrRawValues: Array[String],
      attrEncValues: EncodeCredentialAttributes,
      // rev_reg_id: String,
      // rev_status_list: Pointer,
      // ffiCredRevInfoRegDef: Pointer,
      // ffiCredRevInfoRegDefPrivate: Pointer,
      // ffiCredRevInfoRegIdx: Long,
      // ffiCredRevInfoTailsPath: String,
  ): Either[String, Credential] = {
    val tmp = Credential()
    assert(
      (attrRawValues.length == attrRawValues.length) && (attrRawValues.length == attrEncValues.values.length),
      "TODO"
    ) // return a Left
    val ref = new PointerByReference()
    api
      .shim_anoncreds_create_credential(
        cred_def = credDef.cred_def_ptr.getValue,
        cred_def_private = credDef.cred_def_pvt_ptr.getValue,
        cred_offer = credOffer.ref.getValue,
        cred_request = credRequest.ref.getValue,
        attr_names = attrNames,
        attr_names_len = attrNames.length,
        attr_raw_values = attrRawValues,
        attr_raw_values_len = attrRawValues.length,
        attr_enc_values = attrEncValues.values,
        attr_enc_values_len = attrEncValues.values.length,
        rev_reg_id = "",
        rev_status_list = ref.getValue(),
        ffiCredRevInfoRegDef = ref.getValue(),
        ffiCredRevInfoRegDefPrivate = ref.getValue(),
        /*@int64_t*/ ffiCredRevInfoRegIdx = 0,
        ffiCredRevInfoTailsPath = "",
        cred_p = tmp.ref
      )
      .onSuccess(tmp)
  }
}
