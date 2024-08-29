package org.hyperledger.identus.pollux.vc.jwt

import cats.implicits.*
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader, JWSObject, Payload}
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.SignedJWT
import io.circe.*
import io.circe.syntax.*
import org.hyperledger.identus.shared.crypto.{Ed25519KeyPair, Ed25519PublicKey, KmpEd25519KeyOps}
import org.hyperledger.identus.shared.json.Json as JsonUtils
import org.hyperledger.identus.shared.utils.Base64Utils
import scodec.bits.ByteVector
import zio.*

import java.security.*
import java.security.interfaces.ECPublicKey
import java.time.{Instant, ZoneOffset}
import scala.jdk.CollectionConverters.*

sealed trait Proof {
  val id: Option[String] = None
  val `type`: String
  val proofPurpose: String
  val verificationMethod: String
  val created: Option[Instant] = None
  val domain: Option[String] = None
  val challenge: Option[String] = None
  val previousProof: Option[String] = None
  val nonce: Option[String] = None
}

trait DataIntegrityProof extends Proof {
  val proofValue: String
}

object Proof {
  given decodeProof: Decoder[Proof] = new Decoder[Proof] {
    final def apply(c: HCursor): Decoder.Result[Proof] = {
      val decoders: List[Decoder[Proof]] = List(
        Decoder[EddsaJcs2022Proof].widen,
        Decoder[EcdsaSecp256k1Signature2019Proof].widen,
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

object EcdsaSecp256k1Signature2019ProofGenerator {
  private def stripLeadingZero(arr: Array[Byte]): Array[Byte] = {
    if (arr.length == 33 && arr.head == 0) then arr.tail else arr
  }
  def generateProof(payload: Json, signer: ECDSASigner, pk: ECPublicKey): Task[EcdsaSecp256k1Signature2019Proof] = {
    for {
      dataToSign <- ZIO.fromEither(JsonUtils.canonicalizeJsonLDoRdf(payload.spaces2))
      created = Instant.now()
      header = new JWSHeader.Builder(JWSAlgorithm.ES256K)
        .base64URLEncodePayload(false)
        .criticalParams(Set("b64").asJava)
        .build()
      payload = Payload(dataToSign)
      jwsObject = JWSObject(header, payload)
      _ = jwsObject.sign(signer)
      jws = jwsObject.serialize(true)
      x = stripLeadingZero(pk.getW.getAffineX.toByteArray)
      y = stripLeadingZero(pk.getW.getAffineY.toByteArray)
      jwk = JsonWebKey(
        kty = "EC",
        crv = Some("secp256k1"),
        key_ops = Vector("verify"),
        x = Some(Base64Utils.encodeURL(x)),
        y = Some(Base64Utils.encodeURL(y)),
      )
      ecdaSecp256k1VerificationKey2019 = EcdsaSecp256k1VerificationKey2019(
        publicKeyJwk = jwk
      )
      verificationMethodUrl = Base64Utils.createDataUrl(
        ecdaSecp256k1VerificationKey2019.asJson.dropNullValues.noSpaces.getBytes,
        "application/json"
      )
    } yield EcdsaSecp256k1Signature2019Proof(
      jws = jws,
      verificationMethod = verificationMethodUrl,
      created = Some(created),
    )
  }

  def verifyProof(payload: Json, jws: String, pk: ECPublicKey): Task[Boolean] = {
    for {
      dataToVerify <- ZIO.fromEither(JsonUtils.canonicalizeJsonLDoRdf(payload.spaces2))
      verifier = JWTVerification.toECDSAVerifier(pk)
      signedJws = SignedJWT.parse(jws)
      header = signedJws.getHeader
      signature = signedJws.getSignature
      payload = Payload(dataToVerify)
      jwsObject = new SignedJWT(header.toBase64URL, payload.toBase64URL, signature)
      isValid = jwsObject.verify(verifier)
    } yield isValid
  }
}

object EddsaJcs2022ProofGenerator {
  private val ed25519MultiBaseHeader: Array[Byte] = Array(-19, 1) // 0xed01

  private def pkToMultiKey(pk: Ed25519PublicKey): MultiKey = {
    val encoded = pk.getEncoded
    val withHeader = ed25519MultiBaseHeader ++ encoded
    val base58Encoded = ByteVector.view(withHeader).toBase58
    MultiKey(publicKeyMultibase =
      Some(
        MultiBaseString(
          header = MultiBaseString.Header.Base58Btc,
          data = base58Encoded
        )
      )
    )
  }

  private def multiKeytoPk(multiKey: MultiKey): Either[String, Ed25519PublicKey] = {
    for {
      multiBaseStr <- multiKey.publicKeyMultibase.toRight("No public key provided inside MultiKey")
      bytesWithHeader <- multiBaseStr.getBytes
      pkBytes <- Either.cond(
        bytesWithHeader.take(2).sameElements(ed25519MultiBaseHeader),
        bytesWithHeader.drop(2),
        "Invalid multiBaseString header for ed25519"
      )
      maybePk <- Either.cond(
        pkBytes.length == 32,
        KmpEd25519KeyOps.publicKeyFromEncoded(pkBytes),
        "Invalid public key length, must be 32"
      )
      pk <- maybePk.toEither.left.map(_.getMessage)

    } yield pk
  }

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
      multiKey = pkToMultiKey(ed25519KeyPair.publicKey)
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
    dataToVerify = canonicalizedJson.noSpaces.getBytes
    signature <- ZIO
      .fromEither(MultiBaseString.fromString(proofValue).flatMap(_.getBytes))
      .mapError(error =>
        // TODO fix RuntimeException
        ParsingFailure(error, new RuntimeException(error))
      )
    kmmPk <- ZIO
      .fromEither(multiKeytoPk(pk))
      .mapError(error =>
        // TODO fix RuntimeException
        ParsingFailure("Error Parsing MultiBaseString", new RuntimeException("Error Parsing MultiBaseString"))
      )

    isValid = verify(kmmPk, signature, dataToVerify)
  } yield isValid

  private def verify(publicKey: Ed25519PublicKey, signature: Array[Byte], data: Array[Byte]): Boolean = {
    publicKey.verify(data, signature).isSuccess
  }
}

case class EddsaJcs2022Proof(proofValue: String, verificationMethod: String, maybeCreated: Option[Instant])
    extends Proof
    with DataIntegrityProof {
  override val created: Option[Instant] = maybeCreated
  override val `type`: String = "DataIntegrityProof"
  override val proofPurpose: String = "assertionMethod"
  val cryptoSuite: String = "eddsa-jcs-2022"
}

object EddsaJcs2022Proof {
  given proofEncoder: Encoder[EddsaJcs2022Proof] =
    DataIntegrityProofCodecs.proofEncoder[EddsaJcs2022Proof]("eddsa-jcs-2022")

  given proofDecoder: Decoder[EddsaJcs2022Proof] = DataIntegrityProofCodecs.proofDecoder[EddsaJcs2022Proof](
    (proofValue, verificationMethod, created) => EddsaJcs2022Proof(proofValue, verificationMethod, created),
    "eddsa-jcs-2022"
  )
}

case class EcdsaSecp256k1Signature2019Proof(
    jws: String,
    verificationMethod: String,
    override val created: Option[Instant] = None,
    override val challenge: Option[String] = None,
    override val domain: Option[String] = None,
    override val nonce: Option[String] = None
) extends Proof {
  override val `type`: String = "EcdsaSecp256k1Signature2019"
  override val proofPurpose: String = "assertionMethod"
}

object EcdsaSecp256k1Signature2019Proof {

  given proofEncoder: Encoder[EcdsaSecp256k1Signature2019Proof] =
    (proof: EcdsaSecp256k1Signature2019Proof) =>
      Json
        .obj(
          ("id", proof.id.asJson),
          ("type", proof.`type`.asJson),
          ("proofPurpose", proof.proofPurpose.asJson),
          ("verificationMethod", proof.verificationMethod.asJson),
          ("created", proof.created.map(_.atOffset(ZoneOffset.UTC)).asJson),
          ("domain", proof.domain.asJson),
          ("challenge", proof.challenge.asJson),
          ("jws", proof.jws.asJson),
          ("nonce", proof.nonce.asJson),
        )

  given proofDecoder: Decoder[EcdsaSecp256k1Signature2019Proof] =
    (c: HCursor) =>
      for {
        id <- c.downField("id").as[Option[String]]
        `type` <- c.downField("type").as[String]
        proofPurpose <- c.downField("proofPurpose").as[String]
        verificationMethod <- c.downField("verificationMethod").as[String]
        created <- c.downField("created").as[Option[Instant]]
        domain <- c.downField("domain").as[Option[String]]
        challenge <- c.downField("challenge").as[Option[String]]
        jws <- c.downField("jws").as[String]
        nonce <- c.downField("nonce").as[Option[String]]
      } yield {
        EcdsaSecp256k1Signature2019Proof(
          jws = jws,
          verificationMethod = verificationMethod,
          created = created,
          challenge = challenge,
          domain = domain,
          nonce = nonce
        )
      }
}

object DataIntegrityProofCodecs {
  def proofEncoder[T <: DataIntegrityProof](cryptoSuiteValue: String): Encoder[T] = (proof: T) =>
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

  def proofDecoder[T <: DataIntegrityProof](
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
