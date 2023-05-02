package io.iohk.atala.castor.controller.http

import io.iohk.atala.agent.walletapi.model as walletDomain
import io.iohk.atala.agent.walletapi.model.DIDPublicKeyTemplate
import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.api.http.Annotation
import io.iohk.atala.castor.core.model.did as castorDomain
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.shared.utils.Traverse.*
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}

final case class ManagedDID(
    @description(ManagedDID.annotations.did.description)
    @encodedExample(ManagedDID.annotations.did.example)
    did: String,
    @description(ManagedDID.annotations.longFormDid.description)
    @encodedExample(ManagedDID.annotations.longFormDid.example)
    longFormDid: Option[String] = None,
    @description(ManagedDID.annotations.status.description)
    @encodedExample(ManagedDID.annotations.status.example)
    status: String // TODO: use enum
)

object ManagedDID {
  object annotations {
    object did
        extends Annotation[String](
          description = "A managed DID",
          example = "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff"
        )

    object longFormDid
        extends Annotation[String](
          description = "A long-form DID. Mandatory when status is not PUBLISHED and optional when status is PUBLISHED",
          example =
            "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff:Cr4BCrsBElsKBmF1dGgtMRAEQk8KCXNlY3AyNTZrMRIg0opTuxu-zt6aRbT1tPniG4eu4CYsQPM3rrLzvzNiNgwaIIFTnyT2N4U7qCQ78qtWC3-p0el6Hvv8qxG5uuEw-WgMElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIKhBU0eCOO6Vinz_8vhtFSAhYYqrkEXC8PHGxkuIUev8GiAydFHLXb7c22A1Uj_PR21NZp6BCDQqNq2xd244txRgsQ"
        )

    object status
        extends Annotation[String](
          description =
            """A status indicating a publication state of a DID in the wallet (e.g. PUBLICATION_PENDING, PUBLISHED).
          |Does not represent DID a full lifecyle (e.g. deactivated, recovered, updated).""".stripMargin,
          example = "CREATED"
        )
  }

  given encoder: JsonEncoder[ManagedDID] = DeriveJsonEncoder.gen[ManagedDID]
  given decoder: JsonDecoder[ManagedDID] = DeriveJsonDecoder.gen[ManagedDID]
  given schema: Schema[ManagedDID] = Schema.derived

  given Conversion[ManagedDIDDetail, ManagedDID] = { didDetail =>
    val (longFormDID, status) = didDetail.state match {
      case ManagedDIDState.Created(operation) => Some(PrismDID.buildLongFormFromOperation(operation)) -> "CREATED"
      case ManagedDIDState.PublicationPending(operation, _) =>
        Some(PrismDID.buildLongFormFromOperation(operation)) -> "PUBLICATION_PENDING"
      case ManagedDIDState.Published(_, _) => None -> "PUBLISHED"
    }
    ManagedDID(
      did = didDetail.did.toString,
      longFormDid = longFormDID.map(_.toString),
      status = status
    )
  }
}

final case class ManagedDIDPage(
    self: String,
    kind: String = "ManagedDIDPage",
    pageOf: String,
    next: Option[String] = None,
    previous: Option[String] = None,
    contents: Seq[ManagedDID]
)

object ManagedDIDPage {
  given encoder: JsonEncoder[ManagedDIDPage] = DeriveJsonEncoder.gen[ManagedDIDPage]
  given decoder: JsonDecoder[ManagedDIDPage] = DeriveJsonDecoder.gen[ManagedDIDPage]
  given schema: Schema[ManagedDIDPage] = Schema.derived
}

final case class CreateManagedDidRequest(
    documentTemplate: CreateManagedDidRequestDocumentTemplate
)

object CreateManagedDidRequest {
  given encoder: JsonEncoder[CreateManagedDidRequest] = DeriveJsonEncoder.gen[CreateManagedDidRequest]
  given decoder: JsonDecoder[CreateManagedDidRequest] = DeriveJsonDecoder.gen[CreateManagedDidRequest]
  given schema: Schema[CreateManagedDidRequest] = Schema.derived
}

final case class CreateManagedDidRequestDocumentTemplate(
    publicKeys: Seq[ManagedDIDKeyTemplate],
    services: Seq[Service]
)

object CreateManagedDidRequestDocumentTemplate {
  given encoder: JsonEncoder[CreateManagedDidRequestDocumentTemplate] =
    DeriveJsonEncoder.gen[CreateManagedDidRequestDocumentTemplate]
  given decoder: JsonDecoder[CreateManagedDidRequestDocumentTemplate] =
    DeriveJsonDecoder.gen[CreateManagedDidRequestDocumentTemplate]
  given schema: Schema[CreateManagedDidRequestDocumentTemplate] = Schema.derived

  extension (template: CreateManagedDidRequestDocumentTemplate) {
    def toDomain: Either[String, walletDomain.ManagedDIDTemplate] = {
      for {
        services <- template.services.traverse(_.toDomain)
        publicKeys = template.publicKeys.map[DIDPublicKeyTemplate](k => k)
      } yield walletDomain.ManagedDIDTemplate(
        publicKeys = publicKeys,
        services = services
      )
    }
  }
}

enum Purpose {
  case authentication extends Purpose
  case assertionMethod extends Purpose
  case keyAgreement extends Purpose
  case capabilityInvocation extends Purpose
  case capabilityDelegation extends Purpose
}

object Purpose {
  given Conversion[Purpose, VerificationRelationship] = {
    case Purpose.authentication       => VerificationRelationship.Authentication
    case Purpose.assertionMethod      => VerificationRelationship.AssertionMethod
    case Purpose.keyAgreement         => VerificationRelationship.KeyAgreement
    case Purpose.capabilityInvocation => VerificationRelationship.CapabilityInvocation
    case Purpose.capabilityDelegation => VerificationRelationship.CapabilityDelegation
  }

  given Conversion[VerificationRelationship, Purpose] = {
    case VerificationRelationship.Authentication       => Purpose.authentication
    case VerificationRelationship.AssertionMethod      => Purpose.assertionMethod
    case VerificationRelationship.KeyAgreement         => Purpose.keyAgreement
    case VerificationRelationship.CapabilityInvocation => Purpose.capabilityInvocation
    case VerificationRelationship.CapabilityDelegation => Purpose.capabilityDelegation
  }

  given encoder: JsonEncoder[Purpose] = JsonEncoder[String].contramap(_.toString)
  given decoder: JsonDecoder[Purpose] =
    JsonDecoder[String].mapOrFail(s => Purpose.values.find(_.toString == s).toRight(s"Unknown purpose: $s"))
  given schema: Schema[Purpose] = Schema.derivedEnumeration.defaultStringBased
}

@description("key-pair template to add to DID document.")
final case class ManagedDIDKeyTemplate(
    @description(ManagedDIDKeyTemplate.annotations.id.description)
    @encodedExample(ManagedDIDKeyTemplate.annotations.id.example)
    id: String,
    @description(ManagedDIDKeyTemplate.annotations.purpose.description)
    @encodedExample(ManagedDIDKeyTemplate.annotations.purpose.example)
    purpose: Purpose
)

object ManagedDIDKeyTemplate {
  object annotations {
    object id
        extends Annotation[String](
          description = "Identifier of a verification material in the DID Document",
          example = "key-1"
        )

    object purpose
        extends Annotation[Purpose](
          description = "Purpose of the verification material in the DID Document",
          example = VerificationRelationship.Authentication
        )
  }

  given encoder: JsonEncoder[ManagedDIDKeyTemplate] = DeriveJsonEncoder.gen[ManagedDIDKeyTemplate]
  given decoder: JsonDecoder[ManagedDIDKeyTemplate] = DeriveJsonDecoder.gen[ManagedDIDKeyTemplate]
  given schema: Schema[ManagedDIDKeyTemplate] = Schema.derived

  given Conversion[ManagedDIDKeyTemplate, walletDomain.DIDPublicKeyTemplate] = publicKeyTemplate =>
    walletDomain.DIDPublicKeyTemplate(
      id = publicKeyTemplate.id,
      purpose = publicKeyTemplate.purpose
    )
}

final case class CreateManagedDIDResponse(
    @description(CreateManagedDIDResponse.annotations.longFormDid.description)
    @encodedExample(CreateManagedDIDResponse.annotations.longFormDid.example)
    longFormDid: String
)

object CreateManagedDIDResponse {
  object annotations {
    object longFormDid
        extends Annotation[String](
          description = "A long-form DID for the created DID",
          example =
            "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff:Cr4BCrsBElsKBmF1dGgtMRAEQk8KCXNlY3AyNTZrMRIg0opTuxu-zt6aRbT1tPniG4eu4CYsQPM3rrLzvzNiNgwaIIFTnyT2N4U7qCQ78qtWC3-p0el6Hvv8qxG5uuEw-WgMElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIKhBU0eCOO6Vinz_8vhtFSAhYYqrkEXC8PHGxkuIUev8GiAydFHLXb7c22A1Uj_PR21NZp6BCDQqNq2xd244txRgsQ"
        )
  }

  given encoder: JsonEncoder[CreateManagedDIDResponse] = DeriveJsonEncoder.gen[CreateManagedDIDResponse]
  given decoder: JsonDecoder[CreateManagedDIDResponse] = DeriveJsonDecoder.gen[CreateManagedDIDResponse]
  given schema: Schema[CreateManagedDIDResponse] = Schema.derived
}
