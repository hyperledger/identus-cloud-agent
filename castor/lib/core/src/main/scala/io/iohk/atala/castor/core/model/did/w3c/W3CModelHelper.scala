package io.iohk.atala.castor.core.model.did.w3c

import io.iohk.atala.castor.core.model.did.{
  DIDData,
  DIDMetadata,
  PrismDID,
  PublicKey,
  PublicKeyData,
  Service,
  VerificationRelationship
}
import io.iohk.atala.shared.models.HexString
import io.iohk.atala.castor.core.model.did.ServiceType
import io.circe.Json
import io.iohk.atala.castor.core.model.did.ServiceEndpoint
import io.iohk.atala.castor.core.model.did.ServiceEndpoint.UriOrJsonEndpoint
import io.iohk.atala.castor.core.model.did.EllipticCurve

object W3CModelHelper extends W3CModelHelper

private[castor] trait W3CModelHelper {

  extension (didMetadata: DIDMetadata) {
    def toW3C: DIDDocumentMetadataRepr = DIDDocumentMetadataRepr(
      deactivated = didMetadata.deactivated,
      canonicalId = didMetadata.canonicalId.map(_.toString),
      versionId = HexString.fromByteArray(didMetadata.lastOperationHash.toArray).toString
    )
  }

  extension (didData: DIDData) {
    def toW3C(did: PrismDID): DIDDocumentRepr = {
      import VerificationRelationship.*
      val embeddedKeys = didData.publicKeys.map(k => k.toW3C(did, did))
      val keyRefWithPurpose = didData.publicKeys.map(k => k.purpose -> s"${did.toString}#${k.id}")
      val services = didData.services.map(_.toW3C(did))
      DIDDocumentRepr(
        id = did.toString,
        controller = did.toString,
        verificationMethod = embeddedKeys,
        authentication = keyRefWithPurpose.collect { case (Authentication, k) => k },
        assertionMethod = keyRefWithPurpose.collect { case (AssertionMethod, k) => k },
        keyAgreement = keyRefWithPurpose.collect { case (KeyAgreement, k) => k },
        capabilityInvocation = keyRefWithPurpose.collect { case (CapabilityInvocation, k) => k },
        capabilityDelegation = keyRefWithPurpose.collect { case (CapabilityDelegation, k) => k },
        service = services,
        context = deriveContext(embeddedKeys, services)
      )
    }

    // Reference: https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#constructing-a-json-ld-did-document
    private def deriveContext(keys: Seq[PublicKeyRepr], services: Seq[ServiceRepr]): Seq[String] = {
      val mandatoryContext = Seq("https://www.w3.org/ns/did/v1")
      val additionalContext = {
        val keyTypes = keys.map(_.`type`).toSet
        val serviceTypes = services
          .map(_.`type`)
          .flatMap {
            case s: String      => Seq(s)
            case s: Seq[String] => s
          }
          .toSet
        Seq(
          Option.when(keyTypes.contains("JsonWebKey2020"))("https://w3id.org/security/suites/jws-2020/v1"),
          Option.when(serviceTypes.contains("DIDCommMessaging"))("https://didcomm.org/messaging/contexts/v2"),
          Option.when(serviceTypes.contains("LinkedDomains"))(
            "https://identity.foundation/.well-known/did-configuration/v1"
          )
        ).flatten
      }
      val userDefinedContext = didData.context
      mandatoryContext ++ additionalContext ++ userDefinedContext
    }
  }

  extension (service: Service) {
    def toW3C(did: PrismDID): ServiceRepr =
      ServiceRepr(
        id = s"${did.toString}#${service.id}",
        `type` = serviceTypeToW3C(service.`type`),
        serviceEndpoint = serviceEndpointToW3C(service.serviceEndpoint)
      )

    private def serviceTypeToW3C(serviceType: ServiceType): String | Seq[String] = {
      import ServiceType.*
      serviceType match {
        case ServiceType.Single(name)    => name.value
        case names: ServiceType.Multiple => names.values.map(_.value)
      }
    }

    private def serviceEndpointToW3C(serviceEndpoint: ServiceEndpoint): Json = {
      serviceEndpoint match {
        case ServiceEndpoint.Single(uri) =>
          uri match {
            case UriOrJsonEndpoint.Uri(uri)   => Json.fromString(uri.value)
            case UriOrJsonEndpoint.Json(json) => Json.fromJsonObject(json)
          }
        case ep: ServiceEndpoint.Multiple =>
          val uris = ep.values.map {
            case UriOrJsonEndpoint.Uri(uri)   => Json.fromString(uri.value)
            case UriOrJsonEndpoint.Json(json) => Json.fromJsonObject(json)
          }
          Json.arr(uris: _*)
      }
    }
  }

  // FIXME: do we need to support uncompress for OKP key types?
  extension (publicKey: PublicKey) {
    def toW3C(did: PrismDID, controller: PrismDID): PublicKeyRepr = {
      val curve = publicKey.publicKeyData match {
        case PublicKeyData.ECCompressedKeyData(crv, _) => crv
        case PublicKeyData.ECKeyData(crv, _, _)        => crv
      }
      val publicKeyJwk = curve match {
        case EllipticCurve.SECP256K1 => secp256k1Repr(publicKey.publicKeyData)
        case EllipticCurve.ED25519   => okpPublicKeyRepr(publicKey.publicKeyData)
        case EllipticCurve.X25519    => okpPublicKeyRepr(publicKey.publicKeyData)
      }
      PublicKeyRepr(
        id = s"${did.toString}#${publicKey.id}",
        `type` = "JsonWebKey2020",
        controller = controller.toString,
        publicKeyJwk = publicKeyJwk
      )
    }

    private def okpPublicKeyRepr(pk: PublicKeyData): PublicKeyJwk = {
      pk match {
        case PublicKeyData.ECCompressedKeyData(crv, data) =>
          PublicKeyJwk(
            kty = "OKP",
            crv = crv.name,
            x = Some(data.toStringNoPadding),
            y = None
          )
        case PublicKeyData.ECKeyData(crv, _, _) =>
          throw Exception(s"Uncompressed key for curve ${crv.name} is not supported")
      }
    }

    private def secp256k1Repr(pk: PublicKeyData): PublicKeyJwk = {
      pk match {
        case pk: PublicKeyData.ECCompressedKeyData =>
          val uncomporessed = pk.toUncompressedKeyData.getOrElse(
            throw Exception(s"Conversion to uncompress key is not supported for curve ${pk.crv.name}")
          )
          PublicKeyJwk(
            kty = "EC",
            crv = uncomporessed.crv.name,
            x = Some(uncomporessed.x.toStringNoPadding),
            y = Some(uncomporessed.y.toStringNoPadding)
          )
        case PublicKeyData.ECKeyData(crv, x, y) =>
          PublicKeyJwk(
            kty = "EC",
            crv = crv.name,
            x = Some(x.toStringNoPadding),
            y = Some(y.toStringNoPadding)
          )

      }
    }
  }

}
