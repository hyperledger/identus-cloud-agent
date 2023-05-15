package creative.anoncreds

import creative.anoncreds.AnonCreds._
import jnr.ffi.Pointer
import jnr.ffi.byref.{ByteByReference, IntByReference, NumberByReference, PointerByReference}
import jnr.ffi.types.{int64_t, int8_t}

object AnonCredsOps {

  case class FfiCredentialEntry(
      credential: ObjectHandle,
      @int64_t timestamp: Long,
      rev_state: ObjectHandle
  )

  case class FfiCredentialProve(
      @int64_t entry_idx: Long,
      referent: FfiStr,
      @int8_t is_predicate: Int,
      @int8_t reveal: Int
  )

  implicit class Helpers(val api: AnonCreds) extends AnyVal {

    /*
    If successful, returns the type of the object handle *if* the object handle is known
    and None if it is not known.
     */
    def getObjectHandleType(objHandle: Pointer): Either[ErrorCode, Option[String]] = {
      val out = new PointerByReference()
      eitherErrorCodeOr(
        api.anoncreds_object_get_type_name(objHandle, out),
        Option(out.getValue).map(_.getString(0))
      )
    }

    def getNonce: Either[ErrorCode, String] = {

      val buf = new PointerByReference()

      eitherErrorCodeOr(
        api.anoncreds_generate_nonce(buf),
        buf.getValue.getString(0)
      )
    }

    def getJson(p: Pointer): Either[ErrorCode, String] = {

      val buf = new PointerByReference();

      eitherErrorCodeOr(
        api.shim_anoncreds_object_get_json(
          p,
          buf
        ),
        buf.getValue.getString(0)
      )
    }

    def createPresentation(
        presentationRequest: ObjectHandle,
        credentials: Seq[FfiCredentialEntry],
        credentialProve: Seq[FfiCredentialProve],
        selfAttestNames: Seq[String],
        selfAttestValues: Seq[String],
        masterSecret: ObjectHandle,
        schemas: Seq[ObjectHandle],
        schemaIds: Seq[String],
        credentialDefs: Seq[ObjectHandle],
        credentialDefIds: Seq[String],
        presentationPtr: ObjectHandle_star
    ): ErrorCode = {

      val (c_cred, c_ts, c_rev_state) = credentials.map { cred =>
        (cred.credential, cred.timestamp, cred.rev_state)
      }.unzip3

      val (cp_indx, cp_ref, cp_pred) = credentialProve.map { cp =>
        (cp.entry_idx, cp.referent, cp.is_predicate)
      }.unzip3

      val cp_reveal = credentialProve.map(_.reveal)

      api.shim_anoncreds_create_presentation(
        presentationRequest,
        c_cred.toArray,
        c_ts.toArray,
        c_rev_state.toArray,
        credentials.size,
        cp_indx.toArray,
        cp_ref.toArray,
        cp_pred.toArray,
        cp_reveal.toArray,
        credentialProve.size,
        selfAttestNames.toArray,
        selfAttestNames.size,
        selfAttestValues.toArray,
        selfAttestValues.size,
        masterSecret,
        schemas.toArray,
        schemas.size,
        schemaIds.toArray,
        schemaIds.size,
        credentialDefs.toArray,
        credentialDefs.size,
        credentialDefIds.toArray,
        credentialDefIds.size,
        presentationPtr
      )
    }

    def verifyPresentation(
        presentation: ObjectHandle,
        presentationRequest: ObjectHandle,
        schemas: Seq[ObjectHandle],
        schemaIds: Seq[String],
        credentialDefs: Seq[ObjectHandle],
        credentialDefIds: Seq[String],
        revRegDefs: Seq[ObjectHandle],
        revRegDefIds: Seq[String],
        revStatusList: Seq[ObjectHandle],
        presentationResult: StringBuffer
    ): ErrorCode = {

      api.shim_anoncreds_verify_presentation(
        presentation,
        presentationRequest,
        schemas.toArray,
        schemas.size,
        schemaIds.toArray,
        schemaIds.size,
        credentialDefs.toArray,
        credentialDefs.size,
        credentialDefIds.toArray,
        credentialDefIds.size,
        revRegDefs.toArray,
        revRegDefs.size,
        revRegDefIds.toArray,
        revRegDefIds.size,
        revStatusList.toArray,
        revStatusList.size,
        presentationResult
      )
    }
  }

}
