package org.hyperledger.identus.pollux.vc.jwt

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import io.circe.*
import io.circe.syntax.*
import cats.implicits.*
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader, JWSObject, Payload}

import java.time.{Instant, ZoneOffset}
import zio.*
import org.hyperledger.identus.shared.utils.Json as JsonUtils
import org.hyperledger.identus.shared.utils.Base64Utils
import scodec.bits.ByteVector

import scala.util.Try
import java.security.*
import java.security.spec.X509EncodedKeySpec
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.SignedJWT
import scala.jdk.CollectionConverters.*

import java.security.interfaces.ECPublicKey

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

object Proof {
  given decodeProof: Decoder[Proof] = new Decoder[Proof] {
    final def apply(c: HCursor): Decoder.Result[Proof] = {
      val decoders: List[Decoder[Proof]] = List(
        Decoder[EddsaJcs2022Proof].widen,
        Decoder[EcdsaSecp256k1Signature2019Proof].widen
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

object EddsaJcs2022ProofGenerator {
  private val provider = BouncyCastleProviderSingleton.getInstance
  def generateProof(payload: Json, sk: PrivateKey, pk: PublicKey): Task[EddsaJcs2022Proof] = {
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
    } yield EddsaJcs2022Proof(
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

object EcdsaSecp256k1Signature2019ProofGenerator {
  def generateProof(payload: Json, signer: ECDSASigner, pk: ECPublicKey): Task[EcdsaSecp256k1Signature2019Proof] = {
    for {
      dataToSign <- ZIO.fromEither(JsonUtils.canonicalizeJsonLDoRdf(payload.spaces2))
      created = Instant.now()
      header = new JWSHeader.Builder(JWSAlgorithm.ES256K)
        .customParam("b64", false)
        .criticalParams(Set("b64").asJava)
        .build()
      payload = Payload(dataToSign)
      jwsObject = JWSObject(header, payload)
      _ = jwsObject.sign(signer)
      jws = jwsObject.serialize(true)
      x = pk.getW.getAffineX.toByteArray
      y = pk.getW.getAffineY.toByteArray
      jwk = JsonWebKey(
        kty = "EC",
        crv = Some("secp256k1"),
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
      isValid = verifier.verify(header, dataToVerify, signature)
    } yield isValid
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
