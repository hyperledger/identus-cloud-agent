package io.iohk.atala.pollux.anoncreds

import jnr.ffi.Pointer
import jnr.ffi.byref.PointerByReference
import creative.anoncreds.AnonCreds
import creative.anoncreds.ErrorCode
import java.nio.file.Files
import creative.anoncreds.AnonCredsOps
import creative.anoncreds.AnonCredsOps.{FfiCredentialEntry, FfiCredentialProve, Helpers}

case class CredentialDefinition(
    cred_def_ptr: PointerByReference,
    cred_def_pvt_ptr: PointerByReference,
    key_proof_ptr: PointerByReference,
)
case class SchemaRef(ref: PointerByReference) {
  def getPointer = ref.getValue()
  def json = AnonCredsAPI.getJson(ref.getValue)
}

enum SigType:
  case CL extends SigType
  // .... ?

object AnonCredsAPI {

  private implicit val api: AnonCreds = AnonCreds()
  api.anoncreds_set_default_logger()

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
  ): Either[String, CredentialDefinition] = {
    val cred_def_ptr = new PointerByReference()
    val cred_def_pvt_ptr = new PointerByReference()
    val key_proof_ptr = new PointerByReference()

    println(schemaId)
    println(schema.getPointer)
    println("tag")
    println(issuerDid)
    println(sigType.toString)
    println(if (supportRevocation) (0: Byte) else (1: Byte))
    println(cred_def_ptr)
    println(cred_def_pvt_ptr)
    println(key_proof_ptr)

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
        CredentialDefinition(
          cred_def_ptr,
          cred_def_pvt_ptr,
          key_proof_ptr
        )
      )

  }
}
