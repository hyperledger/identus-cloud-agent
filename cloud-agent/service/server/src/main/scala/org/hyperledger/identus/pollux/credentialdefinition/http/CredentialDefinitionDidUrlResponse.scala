package org.hyperledger.identus.pollux.credentialdefinition.http

import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.castor.core.model.did.{DIDUrl, PrismDID}
import org.hyperledger.identus.pollux.core.model
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition
import org.hyperledger.identus.pollux.credentialdefinition.http.CredentialDefinitionDidUrlResponse.annotations as credentialDefinitionResponseAnnotations
import org.hyperledger.identus.pollux.credentialdefinition.http.CredentialDefinitionInnerDefinitionDidUrlResponse.annotations as credentialDefinitionInnerResponseAnnotations
import org.hyperledger.identus.shared.crypto.Sha256Hash
import org.hyperledger.identus.shared.utils.{Base64Utils, Json as JsonUtils}
import sttp.model.Uri
import sttp.model.Uri.*
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{default, description, encodedExample, encodedName}
import zio.json.*
import zio.json.ast.Json

import java.util.UUID
import scala.collection.immutable.ListMap

case class CredentialDefinitionDidUrlResponse(
    @description(credentialDefinitionResponseAnnotations.resource.description)
    @encodedExample(credentialDefinitionResponseAnnotations.resource.example)
    resource: String,
    @description(credentialDefinitionResponseAnnotations.credentialDefinitionUrl.description)
    @encodedExample(credentialDefinitionResponseAnnotations.credentialDefinitionUrl.example)
    credentialDefinitionUrl: String,
)

object CredentialDefinitionDidUrlResponse {

  def fromDomain(cd: CredentialDefinition, serviceName: String): Either[String, CredentialDefinitionDidUrlResponse] = {
    for {
      authorDid <- PrismDID.fromString(cd.author)
      canonicalized <- JsonUtils.canonicalizeToJcs(cd.toJson).left.map(_.toString)
      encoded = Base64Utils.encodeURL(canonicalized.getBytes)
      hash = Sha256Hash.compute(encoded.getBytes).hexEncoded
      didUrl = DIDUrl(
        authorDid.did,
        Seq(),
        ListMap(
          "resourceService" -> Seq(serviceName),
          "resourcePath" -> Seq(s"credential-definition-registry/definitions/did-url/${cd.guid}?resourceHash=$hash"),
        ),
        None
      ).toString
    } yield CredentialDefinitionDidUrlResponse(
      resource = encoded,
      credentialDefinitionUrl = didUrl
    )
  }

  given encoder: zio.json.JsonEncoder[CredentialDefinitionDidUrlResponse] =
    DeriveJsonEncoder.gen[CredentialDefinitionDidUrlResponse]

  given decoder: zio.json.JsonDecoder[CredentialDefinitionDidUrlResponse] =
    DeriveJsonDecoder.gen[CredentialDefinitionDidUrlResponse]

  given schema: Schema[CredentialDefinitionDidUrlResponse] = Schema.derived

  object annotations {
    object resource
        extends Annotation[String](
          description = "JCS normalized and base64url encoded json credential definition",
          example = "" // TODO Add example
        )

    object credentialDefinitionUrl
        extends Annotation[String](
          description = "DID url that can be used to resolve this credential definition",
          example =
            "did:prism:462c4811bf61d7de25b3baf86c5d2f0609b4debe53792d297bf612269bf8593a?resourceService=agent-base-url&resourcePath=credential-definition-registry/definitions/did-url/ef3e4135-8fcf-3ce7-b5bb-df37defc13f6?resourceHash=4074bb1a8e0ea45437ad86763cd7e12de3fe8349ef19113df773b0d65c8a9c46"
        )
  }

}

case class CredentialDefinitionInnerDefinitionDidUrlResponse(
    @description(credentialDefinitionInnerResponseAnnotations.resource.description)
    @encodedExample(credentialDefinitionInnerResponseAnnotations.resource.example)
    resource: String,
    @description(credentialDefinitionInnerResponseAnnotations.credentialDefinitionUrl.description)
    @encodedExample(credentialDefinitionInnerResponseAnnotations.credentialDefinitionUrl.example)
    credentialDefinitionUrl: String,
)

object CredentialDefinitionInnerDefinitionDidUrlResponse {

  def fromDomain(
      innerDefinition: Json,
      authorDid: PrismDID,
      definitionGuid: UUID,
      serviceName: String
  ): Either[String, CredentialDefinitionInnerDefinitionDidUrlResponse] = {
    for {
      canonicalized <- JsonUtils.canonicalizeToJcs(innerDefinition.toJson).left.map(_.toString)
      encoded = Base64Utils.encodeURL(canonicalized.getBytes)
      hash = Sha256Hash.compute(encoded.getBytes).hexEncoded
      didUrl = DIDUrl(
        authorDid.did,
        Seq(),
        ListMap(
          "resourceService" -> Seq(serviceName),
          "resourcePath" -> Seq(
            s"credential-definition-registry/definitions/did-url/$definitionGuid/definition?resourceHash=$hash"
          ),
        ),
        None
      ).toString
    } yield CredentialDefinitionInnerDefinitionDidUrlResponse(
      resource = encoded,
      credentialDefinitionUrl = didUrl
    )
  }

  object annotations {
    object resource
        extends Annotation[String](
          description =
            "JCS normalized and base64url encoded inner json definition of the credential definition (without metadata)",
          example = "" // TODO Add example
        )

    object credentialDefinitionUrl
        extends Annotation[String](
          description = "DID url that can be used to resolve this schema inner schema",
          example =
            "did:prism:462c4811bf61d7de25b3baf86c5d2f0609b4debe53792d297bf612269bf8593a?resourceService=agent-base-url&resourcePath=credential-definition-registry/definitions/did-url/definition/ef3e4135-8fcf-3ce7-b5bb-df37defc13f6?resourceHash=4074bb1a8e0ea45437ad86763cd7e12de3fe8349ef19113df773b0d65c8a9c46"
        )
  }

  given encoder: JsonEncoder[CredentialDefinitionInnerDefinitionDidUrlResponse] =
    DeriveJsonEncoder.gen[CredentialDefinitionInnerDefinitionDidUrlResponse]
  given decoder: JsonDecoder[CredentialDefinitionInnerDefinitionDidUrlResponse] =
    DeriveJsonDecoder.gen[CredentialDefinitionInnerDefinitionDidUrlResponse]
  given schema: Schema[CredentialDefinitionInnerDefinitionDidUrlResponse] = Schema.derived
}
