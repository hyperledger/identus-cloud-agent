package io.iohk.atala.castor.core.model.did.w3c

import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  DIDData,
  PublicKey,
  PublicKeyData,
  Service,
  VerificationRelationship
}

object W3CModelHelper extends W3CModelHelper

private[castor] trait W3CModelHelper {

  extension (didData: DIDData) {
    def toW3C: DIDDocumentRepr = {
      val keyWithPurpose = didData.publicKeys.map(k => k.purpose -> k.toW3C(didData.id))
      DIDDocumentRepr(
        id = didData.id.toString,
        controller = didData.id.toString,
        verificationMethod = Nil,
        authentication = keyWithPurpose.collect { case (VerificationRelationship.Authentication, k) => k },
        assertionMethod = keyWithPurpose.collect { case (VerificationRelationship.AssertionMethod, k) => k },
        keyAgreement = keyWithPurpose.collect { case (VerificationRelationship.KeyAgreement, k) => k },
        capabilityInvocation = keyWithPurpose.collect { case (VerificationRelationship.CapabilityInvocation, k) => k },
        service = didData.services.map(_.toW3C)
      )
    }
  }

  extension (service: Service) {
    def toW3C: ServiceRepr = ServiceRepr(
      id = service.id,
      `type` = service.`type`.name,
      serviceEndpoint = service.serviceEndpoint.toString
    )
  }

  extension (publicKey: PublicKey) {
    def toW3C(controller: CanonicalPrismDID): PublicKeyRepr = PublicKeyRepr(
      id = publicKey.id,
      `type` = "EcdsaSecp256k1VerificationKey2019",
      controller = controller.toString,
      publicKeyJwk = publicKey.publicKeyData match {
        case PublicKeyData.ECKeyData(crv, x, y) =>
          Some(
            PublicKeyJwk(
              kty = "EC",
              crv = crv.name,
              x = x.toString,
              y = y.toString
            )
          )
      }
    )
  }

}
