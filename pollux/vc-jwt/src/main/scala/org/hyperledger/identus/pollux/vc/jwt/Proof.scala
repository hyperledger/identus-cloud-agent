package org.hyperledger.identus.pollux.vc.jwt

import cats.implicits._
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import io.circe._
import io.circe.syntax._
import org.hyperledger.identus.shared.crypto.Ed25519KeyPair
import org.hyperledger.identus.shared.utils.{Base64Utils, Json => JsonUtils}
import scodec.bits.ByteVector
import zio._

import java.security._
import java.security.spec.X509EncodedKeySpec
import java.time.{Instant, ZoneOffset}
import scala.util.Try

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

object Proof {
  given decodeProof: Decoder[Proof] = new Decoder[Proof] {
    final def apply(c: HCursor): Decoder.Result[Proof] = {
      val decoders: List[Decoder[Proof]] = List(
        Decoder[EcdsaJcs2019Proof].widen
          // Note: Add another proof types here when available
      )

      decoders.foldLeft(
        Left[DecodingFailure, Proof](DecodingFailure("Cannot decode as Proof", c.history)): Decoder.Result[Proof]
      ) { (acc, decoder) =>
        acc.orElse(decoder.tryDecode(c))
      }
    }
  }
}

object EcdsaJcs2019ProofGenerator {
  private val provider = BouncyCastleProviderSingleton.getInstance
  def generateProof(payload: Json, sk: PrivateKey, pk: PublicKey): Task[EcdsaJcs2019Proof] = {
    for {
      canonicalizedJsonString <- ZIO.fromEither(JsonUtils.canonicalizeToJcs(payload.spaces2))
      canonicalizedJson <- ZIO.fromEither(parser.parse(canonicalizedJsonString))
      dataToSign = canonicalizedJson.noSpaces.getBytes
      signature = sign(sk, dataToSign)
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
    } yield EcdsaJcs2019Proof(
      proofValue = base58BtsEncodedSignature,
      maybeCreated = Some(created),
      verificationMethod = verificationMethod
    )
  }

  def verifyProof(payload: Json, proofValue: String, pk: MultiKey): Task[Boolean] = {

    val res = for {
      canonicalizedJsonString <- ZIO
        .fromEither(JsonUtils.canonicalizeToJcs(payload.spaces2))
        .mapError(_.getMessage)
      canonicalizedJson <- ZIO
        .fromEither(parser.parse(canonicalizedJsonString))
        .mapError(_.getMessage)
      dataToVerify = canonicalizedJson.noSpaces.getBytes
      signature <- ZIO.fromEither(MultiBaseString.fromString(proofValue).flatMap(_.getBytes))
      publicKeyBytes <- ZIO.fromEither(
        pk.publicKeyMultibase.toRight("No public key provided inside MultiKey").flatMap(_.getBytes)
      )
      javaPublicKey <- ZIO.fromEither(recoverPublicKey(publicKeyBytes))
      isValid = verify(javaPublicKey, signature, dataToVerify)

    } yield isValid

    res.mapError(e => Throwable(e))
  }

  private def sign(privateKey: PrivateKey, data: Array[Byte]): Array[Byte] = {

    val signer = Signature.getInstance("SHA256withECDSA", provider)
    signer.initSign(privateKey)
    signer.update(data)
    signer.sign()
  }

  private def recoverPublicKey(pkBytes: Array[Byte]): Either[String, PublicKey] = {
    val keyFactory = KeyFactory.getInstance("EC", provider)
    val x509KeySpec = X509EncodedKeySpec(pkBytes)
    Try(keyFactory.generatePublic(x509KeySpec)).toEither.left.map(_.getMessage)
  }

  private def verify(publicKey: PublicKey, signature: Array[Byte], data: Array[Byte]): Boolean = {
    val verifier = Signature.getInstance("SHA256withECDSA", provider)
    verifier.initVerify(publicKey)
    verifier.update(data)
    verifier.verify(signature)
  }
}

object EddsaJcs2022ProofGenerator {
  private val provider = BouncyCastleProviderSingleton.getInstance

  def generateProof(payload: Json, ed25519KeyPair: Ed25519KeyPair): Task[EddsaJcs2022Proof] = {
    for {
      canonicalizedJsonString <- ZIO.fromEither(JsonUtils.canonicalizeToJcs(payload.spaces2))
      canonicalizedJson <- ZIO.fromEither(parser.parse(canonicalizedJsonString))
      dataToSign = canonicalizedJson.noSpaces.getBytes
      signature = ed25519KeyPair.privateKey.sign(dataToSign)
      base58BtsEncodedSignature = MultiBaseString(
        header = MultiBaseString.Header.Base58Btc,
        data = ByteVector.view(signature).toBase58
      ).toMultiBaseString
      created = Instant.now()
      multiKey = MultiKey(publicKeyMultibase =
        Some(
          MultiBaseString(
            header = MultiBaseString.Header.Base64Url,
            data = Base64Utils.encodeURL(ed25519KeyPair.publicKey.getEncoded)
          )
        )
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

  def verifyProof(payload: Json, proofValue: String, pk: MultiKey): IO[ParsingFailure, Boolean] = for {
    canonicalizedJsonString <- ZIO
      .fromEither(JsonUtils.canonicalizeToJcs(payload.spaces2))
      .mapError(ioError => ParsingFailure("Error Parsing canonicalized", ioError))
    canonicalizedJson <- ZIO
      .fromEither(parser.parse(canonicalizedJsonString))
    // .mapError(_.getMessage)
    dataToVerify = canonicalizedJson.noSpaces.getBytes
    signature <- ZIO
      .fromEither(MultiBaseString.fromString(proofValue).flatMap(_.getBytes))
      .mapError(error =>
        // TODO fix RuntimeException
        ParsingFailure("Error Parsing MultiBaseString", new RuntimeException("Error Parsing MultiBaseString"))
      )
    publicKeyBytes <- ZIO
      .fromEither(pk.publicKeyMultibase.toRight("No public key provided inside MultiKey").flatMap(_.getBytes))
      .mapError(error =>
        // TODO fix RuntimeException
        ParsingFailure("Error Parsing MultiBaseString", new RuntimeException("Error Parsing MultiBaseString"))
      )
    javaPublicKey <- ZIO
      .fromEither(recoverPublicKey(publicKeyBytes))
      .mapError(error =>
        // TODO fix RuntimeException
        ParsingFailure("Error recoverPublicKey", new RuntimeException("Error recoverPublicKey"))
      )
    isValid = verify(javaPublicKey, signature, dataToVerify)
  } yield isValid

  private def recoverPublicKey(pkBytes: Array[Byte]): Either[String, PublicKey] = {
    val keyFactory = KeyFactory.getInstance("Ed25519", provider)
    val x509KeySpec = X509EncodedKeySpec(pkBytes)
    Try(keyFactory.generatePublic(x509KeySpec)).toEither.left.map(_.getMessage)
  }

  private def verify(publicKey: PublicKey, signature: Array[Byte], data: Array[Byte]): Boolean = {
    val verifier = Signature.getInstance("Ed25519", provider)
    verifier.initVerify(publicKey)
    verifier.update(data)
    verifier.verify(signature)
  }
}
case class EcdsaJcs2019Proof(proofValue: String, verificationMethod: String, maybeCreated: Option[Instant])
    extends Proof {
  override val created: Option[Instant] = maybeCreated
  override val `type`: String = "DataIntegrityProof"
  override val proofPurpose: String = "assertionMethod"
  val cryptoSuite: String = "ecdsa-jcs-2019"
}

case class EddsaJcs2022Proof(proofValue: String, verificationMethod: String, maybeCreated: Option[Instant])
    extends Proof {
  override val created: Option[Instant] = maybeCreated
  override val `type`: String = "DataIntegrityProof"
  override val proofPurpose: String = "assertionMethod"
  val cryptoSuite: String = "eddsa-jcs-2022"
}

object ProofCodecs {
  def proofEncoder[T <: Proof](cryptoSuiteValue: String): Encoder[T] = (proof: T) =>
    Json.obj(
      ("id", proof.id.asJson),
      ("type", proof.`type`.asJson),
      ("proofPurpose", proof.proofPurpose.asJson),
      ("verificationMethod", proof.verificationMethod.asJson),
      ("created", proof.created.map(_.atOffset(ZoneOffset.UTC)).asJson),
      ("domain", proof.domain.asJson),
      ("challenge", proof.challenge.asJson),
      ("proofValue", proof.proofValue.asJson),
      ("cryptoSuite", Json.fromString(cryptoSuiteValue)),
      ("previousProof", proof.previousProof.asJson),
      ("nonce", proof.nonce.asJson)
    )

  def proofDecoder[T <: Proof](
      createProof: (String, String, Option[Instant]) => T,
      cryptoSuiteValue: String
  ): Decoder[T] =
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
      } yield createProof(proofValue, verificationMethod, created)
}

object EcdsaJcs2019Proof {
  given proofEncoder: Encoder[EcdsaJcs2019Proof] = ProofCodecs.proofEncoder[EcdsaJcs2019Proof]("ecdsa-jcs-2019")
  given proofDecoder: Decoder[EcdsaJcs2019Proof] = ProofCodecs.proofDecoder[EcdsaJcs2019Proof](
    (proofValue, verificationMethod, created) => EcdsaJcs2019Proof(proofValue, verificationMethod, created),
    "ecdsa-jcs-2019"
  )
}

object EddsaJcs2022Proof {
  given proofEncoder: Encoder[EddsaJcs2022Proof] = ProofCodecs.proofEncoder[EddsaJcs2022Proof]("eddsa-jcs-2022")
  given proofDecoder: Decoder[EddsaJcs2022Proof] = ProofCodecs.proofDecoder[EddsaJcs2022Proof](
    (proofValue, verificationMethod, created) => EddsaJcs2022Proof(proofValue, verificationMethod, created),
    "eddsa-jcs-2022"
  )
}
