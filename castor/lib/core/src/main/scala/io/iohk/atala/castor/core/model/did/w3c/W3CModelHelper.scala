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
import io.iohk.atala.shared.models.HexStrings.*

object W3CModelHelper extends W3CModelHelper

private[castor] trait W3CModelHelper {

  extension (didMetadata: DIDMetadata) {
    def toW3C(did: PrismDID): DIDDocumentMetadataRepr = DIDDocumentMetadataRepr(
      deactivated = didMetadata.deactivated,
      canonicalId = did.asCanonical.toString,
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
        val serviceTypes = services.map(_.`type`).toSet
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
    def toW3C(did: PrismDID): ServiceRepr = ServiceRepr(
      id = s"${did.toString}#${service.id}",
      `type` = service.`type`.name,
      serviceEndpoint = service.serviceEndpoint.map(_.toString)
    )
  }

  extension (publicKey: PublicKey) {
    def toW3C(did: PrismDID, controller: PrismDID): PublicKeyRepr = PublicKeyRepr(
      id = s"${did.toString}#${publicKey.id}",
      `type` = "EcdsaSecp256k1VerificationKey2019",
      controller = controller.toString,
      publicKeyJwk = publicKey.publicKeyData match {
        case PublicKeyData.ECKeyData(crv, x, y) =>
          PublicKeyJwk(
            kty = "EC",
            crv = crv.name,
            x = x.toStringNoPadding,
            y = y.toStringNoPadding
          )
        case pk: PublicKeyData.ECCompressedKeyData =>
          val uncompressed = pk.toUncompressedKeyData
          PublicKeyJwk(
            kty = "EC",
            crv = uncompressed.crv.name,
            x = uncompressed.x.toStringNoPadding,
            y = uncompressed.y.toStringNoPadding
          )
      }
    )
  }

}
