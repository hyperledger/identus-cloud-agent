package io.iohk.atala.resolvers

import org.didcommx.didcomm.common.{VerificationMaterial, VerificationMaterialFormat, VerificationMethodType}
import org.didcommx.didcomm.diddoc._

import java.util.Optional
import scala.jdk.CollectionConverters._

object UniversalDidResolver extends DIDDocResolver {
  val authentications = Seq("did:example:alice#key-3").asJava
  val keyAgreements = Seq("did:example:alice#key-agreement-1").asJava
  val verficationMethods = Seq(
    new VerificationMethod(
      "did:example:alice#key-3",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
           "kty":"EC",
           "crv":"secp256k1",
           "x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
           "y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
        }
    """.stripMargin
      ),
      "did:example:alice#key-3"
    ),
    new VerificationMethod(
      "did:example:alice#key-agreement-1",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
                   "kty":"OKP",
                   "crv":"X25519",
                   "x":"avH0O2Y4tqLAq8y9zpianr8ajii5m4F_mICrzNlatXs"
                }
            """.stripMargin
      ),
      "did:example:alice#key-agreement-1"
    )
  ).asJava
  val didCommServices = Seq.empty[DIDCommService].asJava
  val didDocAlice = new DIDDoc(
    "did:example:alice",
    keyAgreements,
    authentications,
    verficationMethods,
    didCommServices
  )

  // DID DOC Bob
  val authenticationBob = Seq("did:example:bob#key-3").asJava
  val keyAgreementsBob = Seq("did:example:bob#key-agreement-1", "did:example:bob#key-agreement-2").asJava
  val verficationMethodsBob = Seq(
    new VerificationMethod(
      "did:example:bob#key-3",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
           "kty":"EC",
           "crv":"secp256k1",
           "x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
           "y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
        }
    """.stripMargin
      ),
      "did:example:bob#key-3"
    ),
    new VerificationMethod(
      "did:example:bob#key-agreement-1",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
                   "kty":"OKP",
                   "crv":"X25519",
                   "x":"GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
                }
            """.stripMargin
      ),
      "did:example:bob#key-agreement-1"
    ),
    new VerificationMethod(
      "did:example:bob#key-agreement-2",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
                   "kty":"OKP",
                   "crv":"X25519",
                   "x":"UT9S3F5ep16KSNBBShU2wh3qSfqYjlasZimn0mB8_VM"
                }
            """.stripMargin
      ),
      "did:bob:mediator#key-agreement-2"
    )
  ).asJava
  val didCommServicesBob = Seq.empty[DIDCommService].asJava
  val didDocBob = new DIDDoc(
    "did:example:mediator",
    keyAgreementsBob,
    authenticationBob,
    verficationMethodsBob,
    didCommServicesBob
  )

  // DID DOC Mediator
  val authenticationMediator = Seq("did:example:mediator#key-3").asJava
  val keyAgreementsMediator = Seq("did:example:mediator#key-agreement-1", "did:example:mediator#key-agreement-2").asJava
  val verficationMethodsMediator = Seq(
    new VerificationMethod(
      "did:example:mediator#key-3",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
           "kty":"EC",
           "crv":"secp256k1",
           "x":"aToW5EaTq5mlAf8C5ECYDSkqsJycrW-e1SQ6_GJcAOk",
           "y":"JAGX94caA21WKreXwYUaOCYTBMrqaX4KWIlsQZTHWCk"
        }
    """.stripMargin
      ),
      "did:example:mediator#key-3"
    ),
    new VerificationMethod(
      "did:example:mediator#key-agreement-1",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
                   "kty":"OKP",
                   "crv":"X25519",
                   "x":"GDTrI66K0pFfO54tlCSvfjjNapIs44dzpneBgyx0S3E"
                }
            """.stripMargin
      ),
      "did:example:mediator#key-agreement-1"
    ),
    new VerificationMethod(
      "did:example:mediator#key-agreement-2",
      VerificationMethodType.JSON_WEB_KEY_2020,
      new VerificationMaterial(
        VerificationMaterialFormat.JWK,
        """{
                   "kty":"OKP",
                   "crv":"X25519",
                   "x":"UT9S3F5ep16KSNBBShU2wh3qSfqYjlasZimn0mB8_VM"
                }
            """.stripMargin
      ),
      "did:example:mediator#key-agreement-2"
    )
  ).asJava
  val didCommServicesMediator = Seq.empty[DIDCommService].asJava
  val didDocMediator = new DIDDoc(
    "did:example:mediator",
    keyAgreementsMediator,
    authenticationMediator,
    verficationMethodsMediator,
    didCommServicesMediator
  )

  val diddocs = Map("did:example:alice" -> didDocAlice, "did:example:mediator" -> didDocMediator, "did:example:bob" -> didDocBob).asJava
  override def resolve(did: String): Optional[DIDDoc] = new DIDDocResolverInMemory(diddocs).resolve(did)
}
