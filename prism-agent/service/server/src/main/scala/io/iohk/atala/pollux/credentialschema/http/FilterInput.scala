package io.iohk.atala.pollux.credentialschema.http

import io.iohk.atala.pollux.core.model
import sttp.tapir.EndpointIO.annotations.{example, query}
import io.iohk.atala.pollux.credentialschema.http.FilterInput.annotations
import sttp.tapir.Validator.*
import io.iohk.atala.api.http.*

case class FilterInput(
    @query
    @example(Option(annotations.author.example))
    author: Option[String] = Option.empty[String],
    @query
    @example(Option(annotations.name.example))
    name: Option[String] = Option.empty[String],
    @query
    @example(Option(annotations.version.example))
    version: Option[String] = Option.empty[String],
    @query
    @example(annotations.tags.example.headOption)
    tags: Option[String] = Option.empty[String]
) {
  def toDomain = model.CredentialSchema.Filter(author, name, version, tags)
}

object FilterInput {
  // TODO: for some reason @description attribute doesn't work together with @query in tapir
  // need to invest more time and probably report the issue
  object annotations {

    object author
        extends Annotation[String](
          description =
            "An optional field that can be used to filter the credential schema collection by `author`'s DID",
          example = CredentialSchemaResponse.annotations.author.example
        )

    object name
        extends Annotation[String](
          description = "An optional field that can be used to filter the credential schema records by `name`",
          example = CredentialSchemaResponse.annotations.name.example
        )

    object version
        extends Annotation[String](
          description = "An optional string field that can be used to filter resources by version",
          example = CredentialSchemaResponse.annotations.version.example
        )

    object tags
        extends Annotation[Seq[String]](
          description = "An optional string field that can be used to filter resources by tags",
          example = CredentialSchemaResponse.annotations.tags.example
        )
  }
}
