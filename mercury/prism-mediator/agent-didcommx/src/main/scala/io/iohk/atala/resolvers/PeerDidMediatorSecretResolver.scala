package io.iohk.atala.resolvers

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.secret.{Secret, SecretResolverInMemory}
import org.didcommx.peerdid.*

import scala.jdk.CollectionConverters.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import org.didcommx.peerdid.core.PeerDIDUtils

object PeerDidMediatorSecretResolver {

  val jwkKeyX25519: OctetKeyPair = new OctetKeyPairGenerator(Curve.X25519)
    //  .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
    .keyID(java.util.UUID.randomUUID.toString()) // give the key a unique ID
    .generate();
  println("********** X25519 ****************")
  println(jwkKeyX25519.toJSONString)
  println(jwkKeyX25519.toPublicJWK)

  val jwkKeyEd25519: OctetKeyPair = new OctetKeyPairGenerator(Curve.Ed25519)
    .keyID(java.util.UUID.randomUUID.toString()) // give the key a unique ID
    .generate();
  println("********** Ed25519 ****************")

  println(jwkKeyEd25519.toJSONString)
  println(jwkKeyEd25519.toPublicJWK)

  val keyAgreement = VerificationMaterialPeerDID[VerificationMethodTypeAgreement](
    VerificationMaterialFormatPeerDID.JWK,
    jwkKeyX25519.toPublicJWK,
    VerificationMethodTypeAgreement.JSON_WEB_KEY_2020.INSTANCE
  )

  val keyAuthentication = VerificationMaterialPeerDID[VerificationMethodTypeAuthentication](
    VerificationMaterialFormatPeerDID.JWK,
    jwkKeyEd25519.toPublicJWK,
    VerificationMethodTypeAuthentication.JSON_WEB_KEY_2020.INSTANCE
  )
  val keyIdAgreement = PeerDIDUtils.createMultibaseEncnumbasis(keyAgreement).drop(1)
  val keyIdAuthentication = PeerDIDUtils.createMultibaseEncnumbasis(keyAuthentication).drop(1)

  val peerDidMediator = org.didcommx.peerdid.PeerDIDCreator.createPeerDIDNumalgo2(
    List(keyAgreement).asJava,
    List(keyAuthentication).asJava,
    null, // service
  )

  private val secretKeyAgreement = new Secret(
    s"$peerDidMediator#$keyIdAgreement",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(VerificationMaterialFormat.JWK, jwkKeyX25519.toJSONString)
  )
  private val secretKeyAuthentication = new Secret(
    s"$peerDidMediator#$keyIdAuthentication",
    VerificationMethodType.JSON_WEB_KEY_2020,
    new VerificationMaterial(VerificationMaterialFormat.JWK, jwkKeyEd25519.toJSONString)
  )

  val secretResolver = new SecretResolverInMemory(
    Map(
      s"$peerDidMediator#$keyIdAgreement" -> secretKeyAgreement,
      s"$peerDidMediator#$keyIdAuthentication" -> secretKeyAuthentication,
    ).asJava
  )

  @main def mediatorPeerDidDoc(): Unit = {
    println(
      org.didcommx.peerdid.PeerDIDResolver
        .resolvePeerDID(
          peerDidMediator,
          VerificationMaterialFormatPeerDID.JWK
        )
    )
  }

}
