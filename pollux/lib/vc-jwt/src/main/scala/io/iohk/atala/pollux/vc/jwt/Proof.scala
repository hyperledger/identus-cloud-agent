package io.iohk.atala.pollux.vc.jwt

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import io.circe.*
import io.circe.syntax.*

import java.time.{Instant, ZoneOffset}
import zio.*
import io.iohk.atala.shared.utils.Json as JsonUtils
import io.iohk.atala.shared.utils.Base64Utils
import scodec.bits.ByteVector
import java.security.*

sealed trait Proof {
  val id: Option[String] = None
  val `type`: String
  val proofPurpose: String
  val verificationMethod: String
  val created: Option[Instant] = None
  val domain: Option[String] = None
  val challenge: Option[String] = None
  val proofValue: String
  val previousProof: Option[String] = None
  val nonce: Option[String] = None
}

trait EddsaJcs2022ProofGenerator {
  private val provider = BouncyCastleProviderSingleton.getInstance
  Security.addProvider(provider)

  def generateProof(payload: Json, sk: PrivateKey, pk: PublicKey): Task[EddsaJcs2022Proof] = {
    for {
      canonicalizedJsonString <- ZIO.fromEither(JsonUtils.canonicalizeToJcs(payload.spaces2))
      canonicalizedJson <- ZIO.fromEither(parser.parse(canonicalizedJsonString))
      signature = sign(sk, canonicalizedJson.noSpaces.getBytes)
      base58BtsEncodedSignature = MultiBaseString(
        header = MultiBaseString.Header.Base58Btc,
        data = ByteVector.view(signature).toBase58
      ).toMultiBaseString
      created = Instant.now()
      multiKey = MultiKey(publicKeyMultibase =
        Some(MultiBaseString(header = MultiBaseString.Header.Base64Url, data = Base64Utils.encodeURL(pk.getEncoded)))
      )
      verificationMethod = Base64Utils.createDataUrl(
        multiKey.asJson.dropNullValues.noSpaces.getBytes,
        "application/json"
      )
    } yield EddsaJcs2022Proof(
      proofValue = base58BtsEncodedSignature,
      maybeCreated = Some(created),
      verificationMethod = verificationMethod
    )
  }

  private def sign(privateKey: PrivateKey, data: Array[Byte]): Array[Byte] = {

    val signer = Signature.getInstance("SHA256withECDSA", provider)
    signer.initSign(privateKey)
    signer.update(data)
    signer.sign()
  }
}
case class EddsaJcs2022Proof(proofValue: String, verificationMethod: String, maybeCreated: Option[Instant])
    extends Proof {
  override val created: Option[Instant] = maybeCreated
  override val `type`: String = "DataIntegrityProof"
  override val proofPurpose: String = "assertionMethod"
  val cryptoSuite: String = "eddsa-jcs-2022"
}

object EddsaJcs2022Proof {

  given proofEncoder: Encoder[EddsaJcs2022Proof] =
    (proof: EddsaJcs2022Proof) =>
      Json
        .obj(
          ("id", proof.id.asJson),
          ("type", proof.`type`.asJson),
          ("proofPurpose", proof.proofPurpose.asJson),
          ("verificationMethod", proof.verificationMethod.asJson),
          ("created", proof.created.map(_.atOffset(ZoneOffset.UTC)).asJson),
          ("domain", proof.domain.asJson),
          ("challenge", proof.challenge.asJson),
          ("proofValue", proof.proofValue.asJson),
          ("cryptoSuite", proof.cryptoSuite.asJson),
          ("previousProof", proof.previousProof.asJson),
          ("nonce", proof.nonce.asJson),
          ("cryptoSuite", proof.cryptoSuite.asJson),
        )
  
  given proofDecoder: Decoder[EddsaJcs2022Proof] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[Option[String]]
        `type` <- c.downField("type").as[String]
        proofPurpose <- c.downField("proofPurpose").as[String]
        verificationMethod <- c.downField("verificationMethod").as[String]
        created <- c.downField("created").as[Option[Instant]]
        domain <- c.downField("domain").as[Option[String]]
        challenge <- c.downField("challenge").as[Option[String]]
        proofValue <- c.downField("proofValue").as[String]
        previousProof <- c.downField("previousProof").as[Option[String]]
        nonce <- c.downField("nonce").as[Option[String]]
        cryptoSuite <- c.downField("cryptoSuite").as[String]
      } yield {
        EddsaJcs2022Proof(
          proofValue = proofValue,
          verificationMethod = verificationMethod,
          maybeCreated = created
        )
      }
}
