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
  def value = ref.getValue()
}

type Schema = Pointer

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
    api.getJson(p) match {
      case Left(value) =>
        throw new RuntimeException(s"json error $value")
      case Right(value) => value
    }

  }

  // def createSchema(
  //     issuerDid: String = "mock:issuer_id/path&q=bar",
  //     attrs: Array[String] = Array("name", "age")
  // ): Pointer = {

  //   val gvtSchemaName = "gvt2"
  //   val schemaVersion = "1.3"

  //   val tails_path = Files.createTempDirectory("tails")
  //   val result_p_int = new PointerByReference()

  //   val result_p = api
  //     .shim_anoncreds_create_schema(
  //       gvtSchemaName,
  //       schemaVersion,
  //       issuerDid,
  //       attrs,
  //       attrs.length,
  //       result_p_int
  //     )
  //     .onSuccess(SchemaRef(result_p_int))

  //   api.getJson(result_p.getValue) match {
  //     case Left(value) => printIfError(value)
  //     case Right(value) =>
  //       println(value)
  //   }
  //   result_p.getValue
  // }

  def createCredentialDefinition(
      schemaId: String,
      schema: Schema,
      issuerDid: String,
      tag: String,
      sigType: SigType = SigType.CL,
      supportRevocation: Boolean = false,
  ): Either[String, CredentialDefinition] = {
    val cred_def_ptr = new PointerByReference()
    val cred_def_pvt_ptr = new PointerByReference()
    val key_proof_ptr = new PointerByReference()

    api
      .anoncreds_create_credential_definition(
        schemaId,
        schema,
        "tag",
        issuerDid,
        sigType.toString,
        if (supportRevocation) 0 else 1,
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
