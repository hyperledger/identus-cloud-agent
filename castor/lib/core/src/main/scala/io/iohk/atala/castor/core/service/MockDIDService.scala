package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.castor.core.model.error
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.keys.ECKeyPair
import io.iohk.atala.shared.models.Base64UrlString
import zio.mock.{Expectation, Mock, Proxy}
import zio.test.Assertion
import zio.{IO, URLayer, ZIO, ZLayer, mock}

import scala.collection.immutable.ArraySeq

object MockDIDService extends Mock[DIDService] {

  object ScheduleOperation extends Effect[SignedPrismDIDOperation, error.DIDOperationError, ScheduleDIDOperationOutcome]
  // FIXME leaving this out for now as it gives a "java.lang.AssertionError: assertion failed: class Array" compilation error
  // object GetScheduledDIDOperationDetail extends Effect[Array[Byte], error.DIDOperationError, Option[ScheduledDIDOperationDetail]]
  object ResolveDID extends Effect[PrismDID, error.DIDResolutionError, Option[(DIDMetadata, DIDData)]]

  override val compose: URLayer[mock.Proxy, DIDService] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new DIDService {
        override def scheduleOperation(
            operation: SignedPrismDIDOperation
        ): IO[error.DIDOperationError, ScheduleDIDOperationOutcome] =
          proxy(ScheduleOperation, operation)

        override def getScheduledDIDOperationDetail(
            operationId: Array[Byte]
        ): IO[error.DIDOperationError, Option[ScheduledDIDOperationDetail]] =
          ???

        override def resolveDID(did: PrismDID): IO[error.DIDResolutionError, Option[(DIDMetadata, DIDData)]] =
          proxy(ResolveDID, did)
      }
    }

  def createDID(
      verificationRelationship: VerificationRelationship
  ): (PrismDIDOperation.Create, ECKeyPair, DIDMetadata, DIDData) = {
    val masterKeyPair = EC.INSTANCE.generateKeyPair()
    val keyPair = EC.INSTANCE.generateKeyPair()
    val createOperation = PrismDIDOperation.Create(
      publicKeys = Seq(
        InternalPublicKey(
          id = "master-0",
          purpose = InternalKeyPurpose.Master,
          publicKeyData = PublicKeyData.ECCompressedKeyData(
            crv = EllipticCurve.SECP256K1,
            data = Base64UrlString.fromByteArray(masterKeyPair.getPublicKey.getEncodedCompressed)
          )
        ),
        PublicKey(
          id = "key-0",
          purpose = verificationRelationship,
          publicKeyData = PublicKeyData.ECCompressedKeyData(
            crv = EllipticCurve.SECP256K1,
            data = Base64UrlString.fromByteArray(keyPair.getPublicKey.getEncodedCompressed)
          )
        ),
      ),
      services = Nil,
      context = Nil,
    )
    val longFormDid = PrismDID.buildLongFormFromOperation(createOperation)
    // val canonicalDid = longFormDid.asCanonical

    val didMetadata =
      DIDMetadata(
        lastOperationHash = ArraySeq.from(longFormDid.stateHash.toByteArray),
        canonicalId = None, // unpublished DID must not contain canonicalId
        deactivated = false, // unpublished DID cannot be deactivated
        created = None, // unpublished DID cannot have timestamp
        updated = None // unpublished DID cannot have timestamp
      )
    val didData = DIDData(
      id = longFormDid.asCanonical,
      publicKeys = createOperation.publicKeys.collect { case pk: PublicKey => pk },
      services = createOperation.services,
      internalKeys = createOperation.publicKeys.collect { case pk: InternalPublicKey => pk },
      context = createOperation.context
    )
    (createOperation, keyPair, didMetadata, didData)
  }

  def resolveDIDExpectation(didMetadata: DIDMetadata, didData: DIDData): Expectation[DIDService] =
    MockDIDService.ResolveDID(
      assertion = Assertion.anything,
      result = Expectation.value(Some(didMetadata, didData))
    )
}
