package io.iohk.atala.castor.core.model.did

import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.AtalaOperation.Operation
import io.iohk.atala.shared.models.Base64UrlString
import io.iohk.atala.shared.models.HexString

import scala.util.Try
import scala.util.matching.Regex

/** Represents a [Did] used in PRISM with prism-specific method and keys as [PrismDid]
  */
sealed trait PrismDID {
  override def toString: String = did.toString

  final def did: DID = DID(PrismDID.PRISM_METHOD, suffix)

  final def asCanonical: CanonicalPrismDID = CanonicalPrismDID(stateHash)

  def stateHash: HexString

  def suffix: DIDMethodSpecificId

}

object PrismDID extends ProtoModelHelper {
  val PRISM_METHOD: DIDMethod = DIDMethod("prism")
  val CANONICAL_SUFFIX_REGEX: Regex = "^([0-9a-f]{64}$)".r
  val LONG_FORM_SUFFIX_REGEX: Regex = "^([0-9a-f]{64}):([A-Za-z0-9_-]+$)".r

  def buildCanonical(stateHash: Array[Byte]): Either[String, CanonicalPrismDID] =
    Try(Sha256Digest.fromBytes(stateHash)).toEither.left
      .map(_.getMessage)
      .map(_ => CanonicalPrismDID(HexString.fromByteArray(stateHash)))

  def buildCanonicalFromSuffix(suffix: String): Either[String, CanonicalPrismDID] =
    HexString
      .fromString(suffix)
      .toEither
      .left
      .map(e => s"unable to parse suffix as hex string: ${e.getMessage}")
      .flatMap(suffix => buildCanonical(suffix.toByteArray))

  def buildLongFormFromOperation(createOperation: PrismDIDOperation.Create): LongFormPrismDID =
    buildLongFormFromAtalaOperation(
      createOperation.toAtalaOperation
    ).toOption.get // This cannot fail because we know the operation is a CreateDid

  def buildLongFormFromAtalaOperation(atalaOperation: node_models.AtalaOperation): Either[String, LongFormPrismDID] =
    atalaOperation.operation match {
      case _: Operation.CreateDid => Right(LongFormPrismDID(atalaOperation))
      case operation =>
        Left(s"Provided initial state of long form Prism DID is ${operation.value}, CreateDid Atala operation expected")
    }

  def fromString(s: String): Either[String, PrismDID] = {
    DID.fromString(s)
      .flatMap { did =>
        if (did.method.toString == PRISM_METHOD.value) Right(did)
        else Left(s"Expected DID to have method ${PRISM_METHOD.value}, but got \"${did.method.toString}\" instead")
      }
      .flatMap { did =>
        val canonicalMatchGroups = CANONICAL_SUFFIX_REGEX.findAllMatchIn(did.methodSpecificId.toString).toList
        val longFormMatchGroups = LONG_FORM_SUFFIX_REGEX.findAllMatchIn(did.methodSpecificId.toString).toList

        (canonicalMatchGroups, longFormMatchGroups) match {
          case (Nil, longFormPattern :: Nil) =>
            for {
              stateHash <- HexString
                .fromString(longFormPattern.group(1))
                .toEither
                .left
                .map(_ => "Invalid long form Prism DID state hash")
              atalaOperation <- Base64UrlString
                .fromString(longFormPattern.group(2))
                .flatMap { encodedStateBase64 =>
                  Try(node_models.AtalaOperation.parseFrom(encodedStateBase64.toByteArray))
                }
                .toEither
                .left
                .map(_ => "Invalid long form Prism DID encoded state")
              longFormDID <- buildLongFormFromAtalaOperation(atalaOperation)
              _ <- Either.cond(
                stateHash == longFormDID.stateHash,
                (),
                "Canonical suffix does not match the computed state"
              )
            } yield longFormDID
          case (canonicalPattern :: Nil, Nil) =>
            HexString
              .fromString(canonicalPattern.group(1))
              .toEither
              .left
              .map(_ => "Invalid canonical form Prism DID")
              .map(stateHash => CanonicalPrismDID(stateHash))
          case _ => Left("Provided DID is not a valid PRISM DID")
        }
      }
  }
}

final case class CanonicalPrismDID private[did] (stateHash: HexString) extends PrismDID {
  override val suffix: DIDMethodSpecificId = DIDMethodSpecificId.fromString(stateHash.toString).get
}

final case class LongFormPrismDID private[did] (atalaOperation: node_models.AtalaOperation) extends PrismDID {

  override val stateHash: HexString = {
    val encodedState = atalaOperation.toByteArray
    HexString.fromByteArray(Sha256.compute(encodedState).getValue)
  }

  override val suffix: DIDMethodSpecificId = {
    val encodedState = Base64UrlString.fromByteArray(atalaOperation.toByteArray).toStringNoPadding
    DIDMethodSpecificId.fromString(s"${stateHash.toString}:${encodedState}").get
  }

  def createOperation: Either[String, PrismDIDOperation.Create] = {
    import ProtoModelHelper.*
    atalaOperation.operation match {
      case Operation.CreateDid(op) => op.toDomain
      case operation =>
        Left(s"Provided initial state of long form Prism DID is ${operation.value}, CreateDid Atala operation expected")
    }
  }

}
