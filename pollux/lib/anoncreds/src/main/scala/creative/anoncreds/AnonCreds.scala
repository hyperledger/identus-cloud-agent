package creative.anoncreds

import creative.anoncreds.AnonCreds.{FfiList_i32, FfiStr, ObjectHandle, ObjectHandle_star}
import jnr.ffi.{LibraryLoader, Pointer, Runtime}
import jnr.ffi.annotations.{In, Out, Pinned}
import jnr.ffi.byref.{ByteByReference, IntByReference, NumberByReference, PointerByReference}
import jnr.ffi.types.{int32_t, int64_t, int8_t, size_t}

/** C function definitions taken from https://github.com/hyperledger/anoncreds-rs/blob/main/include/libanoncreds.h
  */
object AnonCreds {
  type FfiStr = String
  type ObjectHandle = Pointer
  type ObjectHandle_star = PointerByReference
  type FfiList_i32 = Array[Int @int32_t]

  def apply(): AnonCreds = apply(
    Seq(ClasspathSharedObject.createTempFolderWithExtractedLibs.toString)
  )

  def apply(
      pathsToSearch: Seq[String],
      libsToLoad: Seq[String] = ClasspathSharedObject.namesOfSharedObjectsToLoad
  ): AnonCreds = {

    val withPathsToSearch = pathsToSearch.foldLeft(LibraryLoader.create(classOf[AnonCreds])) { case (acc, e) =>
      acc.search(e)
    }
    val withLibsToLoadAndPathsToSearch = libsToLoad.foldLeft(withPathsToSearch) { case (acc, e) =>
      acc.library(e)
    }

    withLibsToLoadAndPathsToSearch.load()

  }

  def eitherErrorCodeOr[T](errorCode: ErrorCode, t: => T): Either[ErrorCode, T] = errorCode match {
    case ErrorCode.SUCCESS => Right(t)
    case err               => Left(err)
  }

}

trait AnonCreds {

  def runtime: Runtime = Runtime.getRuntime(this)

  def anoncreds_version(): String

  def shim_anoncreds_buffer_free(@In buffer: Pointer): Unit;

  def anoncreds_object_free(@In handle: Pointer): Unit

  def shim_anoncreds_encode_credential_attributes(
      @In attr_raw_values: Array[String],
      @In count: Int,
      @Out result_p: PointerByReference
  ): ErrorCode

  def anoncreds_generate_nonce(@Out nonce_p: PointerByReference): ErrorCode

  def anoncreds_get_current_error(@Out error_json_p: PointerByReference): ErrorCode

  def shim_anoncreds_create_credential(
      @In cred_def: Pointer,
      @In cred_def_private: Pointer,
      @In cred_offer: Pointer,
      @In cred_request: Pointer,
      @In attr_names: Array[String],
      @In @size_t attr_names_len: Long,
      @In attr_raw_values: Array[String],
      @In @size_t attr_raw_values_len: Long,
      @In attr_enc_values: Array[String],
      @In @size_t attr_enc_values_len: Long,
      @In rev_reg_id: String,
      @In rev_status_list: Pointer,
      @In ffiCredRevInfoRegDef: Pointer,
      @In ffiCredRevInfoRegDefPrivate: Pointer,
      @In @int64_t ffiCredRevInfoRegIdx: Long,
      @In ffiCredRevInfoTailsPath: String,
      @Out cred_p: PointerByReference
  ): ErrorCode

  def shim_anoncreds_create_schema(
      @In schema_name: String,
      @In schema_version: String,
      @In issuer_id: String,
      @In @Pinned attr_names: Array[String],
      @In attr_names_len: Int,
      @Out result_p: PointerByReference
  ): ErrorCode

  def anoncreds_create_credential_definition(
      @In schema_id: String,
      @size_t schema: Pointer,
      @In tag: String,
      @In issuer_id: String,
      @In signature_type: String,
      @In @int8_t support_revocation: Byte,
      @Out cred_def_p: PointerByReference,
      @Out cred_def_pvt_p: PointerByReference,
      @Out key_proof_p: PointerByReference
  ): ErrorCode

  def shim_anoncreds_object_get_json(@In handle: ObjectHandle, @Out result_p: PointerByReference): ErrorCode

  def anoncreds_create_credential_offer(
      schema_id: FfiStr,
      cred_def_id: FfiStr,
      key_proof: ObjectHandle,
      @Out cred_offer_p: ObjectHandle_star
  ): ErrorCode

  def anoncreds_create_credential_request(
      @In prover_did: FfiStr,
      @In cred_def: ObjectHandle,
      @In master_secret: ObjectHandle,
      @In master_secret_id: FfiStr,
      @In cred_offer: ObjectHandle,
      @Out cred_req_p: ObjectHandle_star,
      @Out cred_req_meta_p: ObjectHandle_star
  ): ErrorCode

  def anoncreds_create_master_secret(@Out master_secret_p: ObjectHandle_star): ErrorCode

  def anoncreds_create_or_update_revocation_state(
      @In rev_reg_def: ObjectHandle,
      @In rev_status_list: ObjectHandle,
      @In @int64_t rev_reg_index: Long,
      @In tails_path: FfiStr,
      @In rev_state: ObjectHandle,
      @In old_rev_status_list: ObjectHandle,
      @Out rev_state_p: ObjectHandle_star
  ): ErrorCode

  def shim_anoncreds_create_presentation(
      pres_req: ObjectHandle,
      credentials_credential: Array[ObjectHandle],
      credentials_timestamp: Array[Long @int64_t],
      credentials_rev_state: Array[ObjectHandle @size_t],
      credentials_count: Long @size_t,
      credentials_prove_entry_idx: Array[Long @int64_t],
      credentials_prove_referent: Array[String],
      credentials_prove_is_predicate: Array[Int @int8_t],
      credentials_prove_is_reveal: Array[Int @int8_t],
      credentials_prove_count: Long @size_t,
      self_attest_names: Array[String],
      self_attest_names_count: Long @size_t,
      self_attest_values: Array[String],
      self_attest_values_count: Long @size_t,
      master_secret: ObjectHandle,
      schemas: Array[ObjectHandle] @int64_t,
      schemas_count: Long @int64_t,
      schema_ids: Array[String],
      schemas_ids_count: Long @size_t,
      cred_defs: Array[ObjectHandle @int64_t],
      cred_defs_count: Long @size_t,
      cred_def_ids: Array[String],
      cred_def_ids_count: Long @size_t,
      @Out presentation_p: ObjectHandle_star
  ): ErrorCode

  def anoncreds_create_revocation_registry_def(
      cred_def: ObjectHandle,
      cred_def_id: FfiStr,
      issuer_id: FfiStr,
      tag: FfiStr,
      rev_reg_type: FfiStr,
      max_cred_num: Long @int64_t,
      tails_dir_path: FfiStr,
      @Out reg_def_p: ObjectHandle_star,
      @Out reg_def_private_p: ObjectHandle_star
  ): ErrorCode

  def anoncreds_create_revocation_status_list(
      rev_reg_def_id: FfiStr,
      rev_reg_def: ObjectHandle,
      timestamp: Long @int64_t,
      issuance_by_default: Int @int8_t,
      @Out rev_status_list_p: ObjectHandle_star
  ): ErrorCode

  def anoncreds_credential_get_attribute(
      handle: ObjectHandle,
      name: FfiStr,
      @Out result_p: PointerByReference
  ): ErrorCode

  def anoncreds_object_get_type_name(handle: ObjectHandle, @Out result_p: PointerByReference): ErrorCode

  def anoncreds_process_credential(
      cred: ObjectHandle,
      cred_req_metadata: ObjectHandle,
      master_secret: ObjectHandle,
      cred_def: ObjectHandle,
      rev_reg_def: ObjectHandle,
      @Out cred_p: ObjectHandle_star
  ): ErrorCode

  def anoncreds_revocation_registry_definition_get_attribute(
      handle: ObjectHandle,
      name: FfiStr,
      @Out result_p: PointerByReference
  ): ErrorCode

  def anoncreds_set_default_logger(): Unit

  def shim_anoncreds_update_revocation_status_list(
      timestamp: Long @int64_t,
      issued: FfiList_i32,
      issued_count: Long @size_t,
      revoked: FfiList_i32,
      revoked_count: Long @int32_t,
      rev_reg_def: ObjectHandle,
      rev_current_list: ObjectHandle,
      @Out new_rev_status_list_p: ObjectHandle_star
  ): ErrorCode

  def anoncreds_update_revocation_status_list_timestamp_only(
      timestamp: Long @int64_t,
      rev_current_list: ObjectHandle,
      @Out rev_status_list_p: ObjectHandle_star
  ): ErrorCode

  def shim_anoncreds_presentation_request_from_json(
      @In json: String,
      @int64_t json_len: Long,
      @Out presentation_req: PointerByReference
  ): ErrorCode

  def shim_anoncreds_verify_presentation(
      presentation: ObjectHandle,
      pres_req: ObjectHandle,
      schemas: Array[ObjectHandle] @int64_t,
      schemas_count: Long @int64_t,
      schema_ids: Array[String],
      schemas_ids_count: Long @size_t,
      cred_defs: Array[ObjectHandle @int64_t],
      cred_defs_count: Long @size_t,
      cred_def_ids: Array[String],
      cred_def_ids_count: Long @size_t,
      rev_reg_defs: Array[ObjectHandle @int64_t],
      rev_reg_defs_count: Long @size_t,
      rev_reg_def_ids: Array[String],
      rev_reg_def_ids_count: Long @size_t,
      rev_status_list: Array[ObjectHandle @int64_t],
      rev_status_list_count: Long @size_t,
      @Out result_p: StringBuffer
  ): ErrorCode

}
