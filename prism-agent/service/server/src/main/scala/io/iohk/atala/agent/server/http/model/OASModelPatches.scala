package io.iohk.atala.agent.server.http.model

import io.iohk.atala.agent.openapi.model.{Service, VerificationMethod, DIDDocumentMetadata, DIDResolutionMetadata}

// Use for overriding models when OpenAPI generator cannot correctly generate models
// The config to patch the generated models is in build.sbt
object OASModelPatches {

  // Need this because the OAS generator cannot generate a case class with a field named '@context'
  final case class DIDDocument(
      `@context`: Seq[String],
      id: String,
      controller: Option[String] = None,
      verificationMethod: Option[Seq[VerificationMethod]] = None,
      authentication: Option[Seq[String]] = None,
      assertionMethod: Option[Seq[String]] = None,
      keyAgreement: Option[Seq[String]] = None,
      capabilityInvocation: Option[Seq[String]] = None,
      capabilityDelegation: Option[Seq[String]] = None,
      service: Option[Seq[Service]] = None
  )

  // Need this because the OAS generator cannot generate a case class with a field named '@context'
  final case class DIDResolutionResult(
      `@context`: String,
      didDocument: Option[DIDDocument] = None,
      didDocumentMetadata: DIDDocumentMetadata,
      didResolutionMetadata: DIDResolutionMetadata
  )

}
