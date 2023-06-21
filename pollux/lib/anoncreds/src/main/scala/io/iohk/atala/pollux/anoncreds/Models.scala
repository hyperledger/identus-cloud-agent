// package io.iohk.atala.pollux.anoncreds

// import jnr.ffi.Pointer
// import jnr.ffi.byref.PointerByReference

// opaque type Tag = String
// object Tag { def apply(s: String): Tag = s }
// extension (x: Tag) def value: String = x

// trait CredentialDefinition {
//   def pub: PointerByReference
//   def keyProof: PointerByReference
// }
// case class CredentialDefinitionPublic(
//     pub: PointerByReference,
//     keyProof: PointerByReference,
// ) extends CredentialDefinition
// case class CredentialDefinitionPrivate(
//     pub: PointerByReference,
//     pvt: PointerByReference,
//     keyProof: PointerByReference,
// ) extends CredentialDefinition {
//   def toPublic = CredentialDefinitionPublic(
//     pub: PointerByReference,
//     pvt: PointerByReference,
//   )
// }

// case class SchemaRef(ref: PointerByReference) {
//   def getPointer = ref.getValue()
//   def json = AnonCredsAPI.getJson(ref.getValue)
// }

// case class RevocationRegistryDefinition(
//     pub: PointerByReference = new PointerByReference(),
//     pvt: PointerByReference = new PointerByReference(),
//     tailsPath: java.nio.file.Path,
// ) {
//   def pathToTailsFileIncName: String = {
//     val json = AnonCredsAPI.getJson(pub.getValue).toOption.get // FIXME
//     // the JSON lib I used failed to parse this first time.
//     val begin = json.indexOf("\"tailsLocation\":") + 17
//     val end = json.substring(begin).indexOf('"')
//     json.substring(begin, begin + end)
//   }
// }

// //{"issuerId":"mock:issuer_id/path&q=bar","revocDefType":"CL_ACCUM","tag":"tag","credDefId":"mock:uri2","value":{"maxCredNum":2,"publicKeys":{"accumKey":{"z":"1 1305B95B284A983D3772E7C03BEFE8F90670D99A7EA230B4591B54629D9F2938 1 08A08D054A819D93EB6E3B43073F962BD90F7348C0FE204F46AFB23223D24AA7 1 2187D529F42E02E20F6317E3545E1B4605240990089A06D7DBB316D104B20D14 1 15236A5DE21C8651B31296F6CF7CE57F09A673A2CF1E744044CCCE9E4468CDA1 1 015402AA6E893F535114238C1022C783F5501DE66D15286A1B795E9582F48716 1 1A462BEA1341BC6AA7A50720226F02D5ADEF559D5E2171853B44C15652401701 1 05433D4BC215D03BFB5FF760B300097926F5447FEDDD83EF800C55AB9BF2984E 1 104EFF5975DBC728393B35923DF2483F9C854E35399200CC70FA5FF142749CA2 1 09F45260D25B8C90D31A071A886077EEBBE03774F5CFECF58FE3B01C2EECA010 1 0F7B2FF52DD3DA26236D7E18F9FD4994FD7E48B304567F454EDBB4BD2EEFA646 1 03E898838251848EAB5773C10276EC2C38EC3BAC05C117745A03A99FD84769BC 1 06814626ABD3980A7109A25D6A533E9EED2D1290BA6A33A9DAFF5EA9EC17804F"}},"tailsHash":"7DhtxByTbgW6StLnfVwtA2fKNsqMDYa8Gu6eB3RXkeBn","tailsLocation":"/tmp/tails3440883722843566857/7DhtxByTbgW6StLnfVwtA2fKNsqMDYa8Gu6eB3RXkeBn"}}

// case class RevocationStatusList(
//     revRegDefId: String,
//     ref: PointerByReference = new PointerByReference()
// )

// enum SigType:
//   case CL extends SigType
//   // .... ?

// enum RevRegType:
//   case CL_ACCUM extends RevRegType
//   // .... ?

// extension (code: ErrorCode)
//   def onSuccess[T](defualt: T) = code match
//     case ErrorCode.SUCCESS                => Right(defualt)
//     case ErrorCode.INPUT                  => Left("Error INPUT")
//     case ErrorCode.IOERROR                => Left("Error IOERROR")
//     case ErrorCode.INVALIDSTATE           => Left("Error INVALIDSTATE")
//     case ErrorCode.UNEXPECTED             => Left("Error UNEXPECTED")
//     case ErrorCode.CREDENTIALREVOKED      => Left("Error CREDENTIALREVOKED")
//     case ErrorCode.INVALIDUSERREVOCID     => Left("Error INVALIDUSERREVOCID")
//     case ErrorCode.PROOFREJECTED          => Left("Error PROOFREJECTED")
//     case ErrorCode.REVOCATIONREGISTRYFULL => Left("Error REVOCATIONREGISTRYFULL")

// case class LinkSecret(ref: PointerByReference = new PointerByReference())
// object LinkSecret {
//   def create: Either[String, LinkSecret] = {
//     val linkSecret = new LinkSecret()
//     AnonCredsAPI.api
//       .anoncreds_create_master_secret(linkSecret.ref)
//       .onSuccess(linkSecret)
//   }
// }

// case class CredentialOffer(
//     ref: PointerByReference = new PointerByReference()
// ) {
//   def json = AnonCredsAPI.getJson(ref.getValue)
// }

// case class CredentialRequest(
//     ref: PointerByReference = new PointerByReference(),
//     meta_ref: PointerByReference = new PointerByReference(),
// )

// case class EncodeCredentialAttributes(ref: PointerByReference = new PointerByReference()) {
//   def values: Array[String] = ref.getValue.getString(0).split(",")
// }
// case class Credential(ref: PointerByReference = new PointerByReference())
