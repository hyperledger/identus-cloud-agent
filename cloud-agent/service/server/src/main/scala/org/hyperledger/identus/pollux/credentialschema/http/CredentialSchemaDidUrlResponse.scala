package org.hyperledger.identus.pollux.credentialschema.http

import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.castor.core.model.did.{DIDUrl, PrismDID}
import org.hyperledger.identus.pollux.core.model
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema
import org.hyperledger.identus.pollux.credentialschema.http.CredentialSchemaDidUrlResponse.annotations as SchemaResponseAnnotations
import org.hyperledger.identus.pollux.credentialschema.http.CredentialSchemaInnerDidUrlResponse.annotations as SchemaResponseInnerAnnotations
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

case class CredentialSchemaDidUrlResponse(
    @description(SchemaResponseAnnotations.resource.description)
    @encodedExample(SchemaResponseAnnotations.resource.example)
    resource: String,
    @description(SchemaResponseAnnotations.schemaUrl.description)
    @encodedExample(SchemaResponseAnnotations.schemaUrl.example)
    schemaUrl: String,
)

object CredentialSchemaDidUrlResponse {

  def fromDomain(cs: CredentialSchema, serviceName: String): Either[String, CredentialSchemaDidUrlResponse] = {

//    Url : TODO: change in codebase with DIDUrl usage
//      .parse(s"${jwtIssuer.did}?resourceService=$statusListRegistryServiceName&resourcePath=$resourcePath#$segment")
//      .toString
//    ,
    for {
      authorDid <- PrismDID.fromString(cs.author)
      canonicalized <- JsonUtils.canonicalizeToJcs(cs.toJson).left.map(_.toString)
      encoded = Base64Utils.encodeURL(canonicalized.getBytes)
      hash = Sha256Hash.compute(encoded.getBytes).hexEncoded
      didUrl = DIDUrl(
        authorDid.did,
        Seq(),
        ListMap(
          "resourceService" -> Seq(serviceName),
          "resourcePath" -> Seq(s"schema-registry/schemas/did-url/${cs.guid}?resourceHash=$hash"),
        ),
        None
      ).toString
    } yield CredentialSchemaDidUrlResponse(
      resource = encoded,
      schemaUrl = didUrl
    )
  }

  given encoder: JsonEncoder[CredentialSchemaDidUrlResponse] =
    DeriveJsonEncoder.gen[CredentialSchemaDidUrlResponse]
  given decoder: JsonDecoder[CredentialSchemaDidUrlResponse] =
    DeriveJsonDecoder.gen[CredentialSchemaDidUrlResponse]
  given schema: Schema[CredentialSchemaDidUrlResponse] = Schema.derived

  object annotations {
    object resource
        extends Annotation[String](
          description = "JCS normalized and base64url encoded json schema",
          example =
            """ewogICJndWlkIjogIjNmODZhNzNmLTViNzgtMzljNy1hZjc3LTBjMTYxMjNmYTljMiIsCiAgImlkIjogImYyYmZiZjc4LThiZDYtNGNjNi04YjM5LWIzYTI1ZTAxZThlYSIsCiAgImxvbmdJZCI6ICJkaWQ6cHJpc206YWdlbnQvZjJiZmJmNzgtOGJkNi00Y2M2LThiMzktYjNhMjVlMDFlOGVhP3ZlcnNpb249MS4wLjAiLAogICJuYW1lIjogImRyaXZpbmctbGljZW5zZSIsCiAgInZlcnNpb24iOiAiMS4wLjAiLAogICJkZXNjcmlwdGlvbiI6ICJEcml2aW5nIExpY2Vuc2UgU2NoZW1hIiwKICAidHlwZSI6ICJodHRwczovL3czYy1jY2cuZ2l0aHViLmlvL3ZjLWpzb24tc2NoZW1hcy9zY2hlbWEvMi4wL3NjaGVtYS5qc29uIiwKICAiYXV0aG9yIjogImRpZDpwcmlzbTo0YTViNWNmMGE1MTNlODNiNTk4YmJlYTI1Y2Q2MTk2NzQ2NzQ3ZjM2MWE3M2VmNzcwNjgyNjhiYzliZDczMmZmIiwKICAiYXV0aG9yZWQiOiAiMjAyMy0wMy0xNFQxNDo0MTo0Ni43MTM5NDNaIiwKICAidGFncyI6IFsKICAgICJkcml2aW5nIiwKICAgICJsaWNlbnNlIgogIF0sCiAgInNjaGVtYSI6IHsKICAgICIkaWQiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9kcml2aW5nLWxpY2Vuc2UtMS4wLjAiLAogICAgIiRzY2hlbWEiOiAiaHR0cHM6Ly9qc29uLXNjaGVtYS5vcmcvZHJhZnQvMjAyMC0xMi9zY2hlbWEiLAogICAgImRlc2NyaXB0aW9uIjogIkRyaXZpbmcgTGljZW5zZSIsCiAgICAidHlwZSI6ICJvYmplY3QiLAogICAgInByb3BlcnRpZXMiOiB7CiAgICAgICJlbWFpbEFkZHJlc3MiOiB7CiAgICAgICAgInR5cGUiOiAic3RyaW5nIiwKICAgICAgICAiZm9ybWF0IjogImVtYWlsIgogICAgICB9LAogICAgICAiZ2l2ZW5OYW1lIjogewogICAgICAgICJ0eXBlIjogInN0cmluZyIKICAgICAgfSwKICAgICAgImZhbWlseU5hbWUiOiB7CiAgICAgICAgInR5cGUiOiAic3RyaW5nIgogICAgICB9LAogICAgICAiZGF0ZU9mSXNzdWFuY2UiOiB7CiAgICAgICAgInR5cGUiOiAic3RyaW5nIiwKICAgICAgICAiZm9ybWF0IjogImRhdGUtdGltZSIKICAgICAgfSwKICAgICAgImRyaXZpbmdMaWNlbnNlSUQiOiB7CiAgICAgICAgInR5cGUiOiAic3RyaW5nIgogICAgICB9LAogICAgICAiZHJpdmluZ0NsYXNzIjogewogICAgICAgICJ0eXBlIjogImludGVnZXIiCiAgICAgIH0KICAgIH0sCiAgICAicmVxdWlyZWQiOiBbCiAgICAgICJlbWFpbEFkZHJlc3MiLAogICAgICAiZmFtaWx5TmFtZSIsCiAgICAgICJkYXRlT2ZJc3N1YW5jZSIsCiAgICAgICJkcml2aW5nTGljZW5zZUlEIiwKICAgICAgImRyaXZpbmdDbGFzcyIKICAgIF0sCiAgICAiYWRkaXRpb25hbFByb3BlcnRpZXMiOiB0cnVlCiAgfQp9"""
        )

    object schemaUrl
        extends Annotation[String](
          description = "DID url that can be used to resolve this schema",
          example =
            "did:prism:462c4811bf61d7de25b3baf86c5d2f0609b4debe53792d297bf612269bf8593a?resourceService=agent-base-url&resourcePath=schema-registry/schemas/did-url/ef3e4135-8fcf-3ce7-b5bb-df37defc13f6?resourceHash=4074bb1a8e0ea45437ad86763cd7e12de3fe8349ef19113df773b0d65c8a9c46"
        )
  }
}

case class CredentialSchemaInnerDidUrlResponse(
    @description(SchemaResponseInnerAnnotations.resource.description)
    @encodedExample(SchemaResponseInnerAnnotations.resource.example)
    resource: String,
    @description(SchemaResponseInnerAnnotations.schemaUrl.description)
    @encodedExample(SchemaResponseInnerAnnotations.schemaUrl.example)
    schemaUrl: String,
)

object CredentialSchemaInnerDidUrlResponse {

  def fromDomain(
      innerSchema: Json,
      authorDid: PrismDID,
      schemaGuid: UUID,
      serviceName: String
  ): Either[String, CredentialSchemaInnerDidUrlResponse] = {
    for {
      canonicalized <- JsonUtils.canonicalizeToJcs(innerSchema.toJson).left.map(_.toString)
      encoded = Base64Utils.encodeURL(canonicalized.getBytes)
      hash = Sha256Hash.compute(encoded.getBytes).hexEncoded
      didUrl = DIDUrl(
        authorDid.did,
        Seq(),
        ListMap(
          "resourceService" -> Seq(serviceName),
          "resourcePath" -> Seq(s"schema-registry/schemas/did-url/$schemaGuid/schema?resourceHash=$hash"),
        ),
        None
      ).toString
    } yield CredentialSchemaInnerDidUrlResponse(
      resource = encoded,
      schemaUrl = didUrl
    )
  }

  object annotations {
    object resource
      extends Annotation[String](
        description = "JCS normalized and base64url encoded inner json schema (without metadata)",
        example =
          """eyIkaWQiOiJodHRwczovL2V4YW1wbGUuY29tL2RyaXZpbmctbGljZW5zZS0xLjAuMCIsIiRzY2hlbWEiOiJodHRwczovL2pzb24tc2NoZW1hLm9yZy9kcmFmdC8yMDIwLTEyL3NjaGVtYSIsImFkZGl0aW9uYWxQcm9wZXJ0aWVzIjp0cnVlLCJkZXNjcmlwdGlvbiI6IkRyaXZpbmcgTGljZW5zZSIsInByb3BlcnRpZXMiOnsiZGF0ZU9mSXNzdWFuY2UiOnsiZm9ybWF0IjoiZGF0ZS10aW1lIiwidHlwZSI6InN0cmluZyJ9LCJkcml2aW5nQ2xhc3MiOnsidHlwZSI6ImludGVnZXIifSwiZHJpdmluZ0xpY2Vuc2VJRCI6eyJ0eXBlIjoic3RyaW5nIn0sImVtYWlsQWRkcmVzcyI6eyJmb3JtYXQiOiJlbWFpbCIsInR5cGUiOiJzdHJpbmcifSwiZmFtaWx5TmFtZSI6eyJ0eXBlIjoic3RyaW5nIn0sImdpdmVuTmFtZSI6eyJ0eXBlIjoic3RyaW5nIn19LCJyZXF1aXJlZCI6WyJlbWFpbEFkZHJlc3MiLCJmYW1pbHlOYW1lIiwiZGF0ZU9mSXNzdWFuY2UiLCJkcml2aW5nTGljZW5zZUlEIiwiZHJpdmluZ0NsYXNzIl0sInR5cGUiOiJvYmplY3QifQ=="""
      )

    object schemaUrl
      extends Annotation[String](
        description = "DID url that can be used to resolve this schema inner schema",
        example =
          "did:prism:462c4811bf61d7de25b3baf86c5d2f0609b4debe53792d297bf612269bf8593a?resourceService=agent-base-url&resourcePath=schema-registry/schemas/did-url/ef3e4135-8fcf-3ce7-b5bb-df37defc13f6&resourceHash=4074bb1a8e0ea45437ad86763cd7e12de3fe8349ef19113df773b0d65c8a9c46/schema"
      )
  }

  given encoder: JsonEncoder[CredentialSchemaInnerDidUrlResponse] =
    DeriveJsonEncoder.gen[CredentialSchemaInnerDidUrlResponse]
  given decoder: JsonDecoder[CredentialSchemaInnerDidUrlResponse] =
    DeriveJsonDecoder.gen[CredentialSchemaInnerDidUrlResponse]
  given schema: Schema[CredentialSchemaInnerDidUrlResponse] = Schema.derived
}
