// package io.iohk.atala.pollux.anoncreds

// import jnr.ffi.Pointer
// import jnr.ffi.byref.PointerByReference
// import creative.anoncreds.AnonCreds
// import creative.anoncreds.ErrorCode
// import java.nio.file.Files
// import creative.anoncreds.AnonCredsOps
// import creative.anoncreds.AnonCredsOps.{FfiCredentialEntry, FfiCredentialProve, Helpers}

// object AnonCredsAPI {

//   val api: AnonCreds = AnonCreds()
//   api.anoncreds_set_default_logger()

//   def version = api.anoncreds_version()

//   def getJson(p: Pointer): Either[String, String] = {

//     val buf = new PointerByReference()

//     val data = api
//       .shim_anoncreds_object_get_json(
//         p,
//         buf
//       )
//       .onSuccess(buf.getValue.getString(0))

//     data

//   }

//   def createSchema(
//       issuerDid: String = "mock:issuer_id/path&q=bar",
//       attrs: Array[String] = Array("name", "age")
//   ): Either[String, SchemaRef] = {

//     val gvtSchemaName = "gvt2"
//     val schemaVersion = "1.3"

//     val tails_path = Files.createTempDirectory("tails")
//     val result_p_int = new PointerByReference()

//     api
//       .shim_anoncreds_create_schema(
//         gvtSchemaName,
//         schemaVersion,
//         issuerDid,
//         attrs,
//         attrs.length,
//         result_p_int
//       )
//       .onSuccess(SchemaRef(result_p_int))
//   }

//   def createCredentialDefinition(
//       schemaId: String,
//       schema: SchemaRef,
//       issuerDid: String,
//       tag: Tag,
//       sigType: SigType = SigType.CL,
//       supportRevocation: Boolean = false,
//   ): Either[String, CredentialDefinitionPrivate] = {
//     val pub = new PointerByReference()
//     val pvt = new PointerByReference()
//     val keyProof = new PointerByReference()

//     // println(schemaId)
//     // println(schema.getPointer)
//     // println("tag")
//     // println(issuerDid)
//     // println(sigType.toString)
//     // println(if (supportRevocation) (0: Byte) else (1: Byte))
//     // println(pub)
//     // println(pvt)
//     // println(keyProof)

//     api
//       .anoncreds_create_credential_definition(
//         schemaId,
//         schema.getPointer,
//         tag.value,
//         issuerDid,
//         sigType.toString,
//         if (supportRevocation) (0: Byte) else (1: Byte),
//         pub,
//         pvt,
//         keyProof
//       )
//       .onSuccess(
//         CredentialDefinitionPrivate(
//           pub,
//           pvt,
//           keyProof
//         )
//       )

//   }

//   def createCredentialOffer(
//       schemaId: String,
//       credDefId: String,
//       credentialDefinition: CredentialDefinitionPrivate
//   ): Either[String, CredentialOffer] = {
//     val offer = CredentialOffer()
//     api
//       .anoncreds_create_credential_offer(
//         schema_id = schemaId,
//         cred_def_id = credDefId,
//         key_proof = credentialDefinition.keyProof.getValue(),
//         cred_offer_p = offer.ref,
//       )
//       .onSuccess(offer)
//   }

//   // *********************************************************************

//   def createCredentialRequest(
//       proverDID: String = null,
//       credDef: CredentialDefinitionPublic,
//       linkSecret: LinkSecret,
//       linkSecretId: String,
//       credOffer: CredentialOffer,
//   ): Either[String, CredentialRequest] = {
//     val tmp = CredentialRequest()

//     api
//       .anoncreds_create_credential_request(
//         proverDID,
//         credDef.pub.getValue,
//         linkSecret.ref.getValue,
//         linkSecretId,
//         credOffer.ref.getValue,
//         tmp.ref,
//         tmp.meta_ref,
//       )
//       .onSuccess(tmp)
//   }

//   def encodeCredentialAttributes(attr: Array[String]): Either[String, EncodeCredentialAttributes] = {
//     val tmp = EncodeCredentialAttributes()
//     api
//       .shim_anoncreds_encode_credential_attributes(
//         attr,
//         attr.length,
//         tmp.ref
//       )
//       .onSuccess(tmp)
//   }

//   def createCredential(
//       credDef: CredentialDefinitionPrivate,
//       credOffer: CredentialOffer,
//       credRequest: CredentialRequest,
//       attrNames: Array[String],
//       attrRawValues: Array[String],
//       attrEncValues: EncodeCredentialAttributes,
//       // rev_reg_id: String,
//       // rev_status_list: Pointer,
//       revocationRegistryDefinition: RevocationRegistryDefinition,
//       revocationStatusList: RevocationStatusList,
//       // 00 ffiCredRevInfoRegDef: Pointer,
//       // 00 ffiCredRevInfoRegDefPrivate: Pointer,
//       // ffiCredRevInfoRegIdx: Long,
//       // ffiCredRevInfoTailsPath: String,
//   ): Either[String, Credential] = {
//     val tmp = Credential()
//     assert(
//       (attrRawValues.length == attrRawValues.length) && (attrRawValues.length == attrEncValues.values.length),
//       "TODO"
//     ) // return a Left
//     val ref = new PointerByReference()

//     // println(s"""
//     // .shim_anoncreds_create_credential(
//     //     cred_def = ${credDef.pub.getValue},
//     //     cred_def_private = ${credDef.pvt.getValue},
//     //     cred_offer = ${credOffer.ref.getValue},
//     //     cred_request = ${credRequest.ref.getValue},
//     //     attr_names = ${attrNames},
//     //     attr_names_len = ${attrNames.length},
//     //     attr_raw_values = ${attrRawValues},
//     //     attr_raw_values_len = ${attrRawValues.length},
//     //     attr_enc_values = ${attrEncValues.values},
//     //     attr_enc_values_len = ${attrEncValues.values.length},
//     //     rev_reg_id = ${revocationStatusList.revRegDefId},
//     //     rev_status_list = ${revocationStatusList.ref.getValue()},
//     //     ffiCredRevInfoRegDef = ${revocationRegistryDefinition.pub.getValue()},
//     //     ffiCredRevInfoRegDefPrivate = ${revocationRegistryDefinition.pvt.getValue()},
//     //     /*@int64_t*/ ffiCredRevInfoRegIdx = ${0L},
//     //     ffiCredRevInfoTailsPath = ${revocationRegistryDefinition.pathToTailsFileIncName},
//     //     cred_p = ${tmp.ref}
//     //   )
//     // """)
//     api
//       .shim_anoncreds_create_credential(
//         cred_def = credDef.pub.getValue,
//         cred_def_private = credDef.pvt.getValue,
//         cred_offer = credOffer.ref.getValue,
//         cred_request = credRequest.ref.getValue,
//         attr_names = attrNames,
//         attr_names_len = attrNames.length,
//         attr_raw_values = attrRawValues,
//         attr_raw_values_len = attrRawValues.length,
//         attr_enc_values = attrEncValues.values,
//         attr_enc_values_len = attrEncValues.values.length,
//         rev_reg_id = revocationStatusList.revRegDefId,
//         rev_status_list = revocationStatusList.ref.getValue(),
//         ffiCredRevInfoRegDef = revocationRegistryDefinition.pub.getValue(),
//         ffiCredRevInfoRegDefPrivate = revocationRegistryDefinition.pvt.getValue(),
//         /*@int64_t*/ ffiCredRevInfoRegIdx = 0L,
//         ffiCredRevInfoTailsPath = revocationRegistryDefinition.pathToTailsFileIncName,
//         cred_p = tmp.ref
//       )
//       .onSuccess(tmp)
//   }

//   // REVOCATION!

//   def createRevocationRegistry(
//       credentialDefinition: CredentialDefinition,
//       credDefId: String,
//       issuerDID: String,
//       tag: Tag,
//   ): Either[String, RevocationRegistryDefinition] = {

//     val tailsPath: java.nio.file.Path = Files.createTempDirectory("tails") // what is this for???
//     val tmp = RevocationRegistryDefinition(tailsPath = tailsPath)

//     api
//       .anoncreds_create_revocation_registry_def(
//         cred_def = credentialDefinition.pub.getValue,
//         cred_def_id = credDefId,
//         issuer_id = issuerDID,
//         tag = tag.value,
//         rev_reg_type = RevRegType.CL_ACCUM.toString(),
//         max_cred_num = 2,
//         tails_dir_path = tailsPath.toString,
//         tmp.pub,
//         tmp.pvt,
//       )
//       .onSuccess(tmp)
//   }

//   def createRevocationStatusList(
//       revRegDefId: String, //  "mock:uri2"
//       revocationRegistryDefinition: RevocationRegistryDefinition,
//       timeStamp: Long = 12L
//   ): Either[String, RevocationStatusList] = {
//     val tmp = RevocationStatusList(revRegDefId = revRegDefId)
//     api
//       .anoncreds_create_revocation_status_list(
//         tmp.revRegDefId,
//         revocationRegistryDefinition.pub.getValue,
//         timeStamp,
//         0, /// not sure what this does, but only 0 seems to work
//         tmp.ref
//       )
//       .onSuccess(tmp)
//   }
// }

// // .shim_anoncreds_create_credential(
// //     cred_def = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x2]
// //     cred_def_private = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x3]
// //     cred_offer = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x5]
// //     cred_request = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x7]
// //     attr_names = [Ljava.lang.String;@21e6b84
// //     attr_names_len = 2
// //     attr_raw_values = [Ljava.lang.String;@2f64e8d7
// //     attr_raw_values_len = 2
// //     attr_enc_values = [Ljava.lang.String;@9607a78
// //     attr_enc_values_len = 2
// //     rev_reg_id = mock:uri2
// //     rev_status_list = jnr.ffi.provider.jffi.DirectMemoryIO[address=0xb]
// //     ffiCredRevInfoRegDef = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x9]
// //     ffiCredRevInfoRegDefPrivate = jnr.ffi.provider.jffi.DirectMemoryIO[address=0xa]
// //     /*@int64_t*/ ffiCredRevInfoRegIdx = 0
// //     ffiCredRevInfoTailsPath = /tmp/tails1892628276345612448/8BzH3Agfy33RuPK5m4nMcqTcnsq5DfCz91b6SUwc8oGp
// //     cred_p = jnr.ffi.byref.PointerByReference@1e1687b
// //   )

// //   .shim_anoncreds_create_credential(
// //     cred_def = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x2],
// //     cred_def_private = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x3],
// //     cred_offer = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x5],
// //     cred_request = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x7],
// //     attr_names = [Ljava.lang.String;@5f39399e,
// //     attr_names_len = 2,
// //     attr_raw_values = [Ljava.lang.String;@3ad5e936,
// //     attr_raw_values_len = 2,
// //     attr_enc_values = [Ljava.lang.String;@58cb96ef,
// //     attr_enc_values_len = 2,
// //     rev_reg_id = mock:uri2,
// //     rev_status_list = jnr.ffi.provider.jffi.DirectMemoryIO[address=0xb],
// //     ffiCredRevInfoRegDef = jnr.ffi.provider.jffi.DirectMemoryIO[address=0x9],
// //     ffiCredRevInfoRegDefPrivate = jnr.ffi.provider.jffi.DirectMemoryIO[address=0xa],
// //     /*@int64_t*/ ffiCredRevInfoRegIdx = 0,
// //     ffiCredRevInfoTailsPath = /tmp/tails596794321566758195,
// //     cred_p = jnr.ffi.byref.PointerByReference@8089af8
// //   )
