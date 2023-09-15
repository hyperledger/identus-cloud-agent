package io.iohk.atala.pollux.anoncreds

import uniffi.anoncreds

import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

/** @see
  *   https://hyperledger.github.io/anoncreds-spec/
  */
object AnoncredLib {

  val SCHEMA_ID = "mock:uri2"
  val CRED_DEF_ID = "mock:uri2"
  val ISSUER_DID = "mock:issuer_id/path&q=bar"

  // issuer or any
  def createSchema(
      name: String, // SCHEMA_ID
      version: String, // SCHEMA_Version
      attr_names: AttributeNames,
      issuer_id: IssuerId, // ISSUER_DID
  ): SchemaDef = anoncreds.Schema.apply(name, version, attr_names.toSeq.asJava, issuer_id)

  // issuer
  def createCredDefinition(
      issuer_id: String,
      schema: SchemaDef,
      tag: String,
      supportRevocation: Boolean,
      signature_type: anoncreds.SignatureType.CL.type = anoncreds.SignatureType.CL
  ) = {
    val credentialDefinition: anoncreds.IssuerCreateCredentialDefinitionReturn =
      anoncreds
        .Issuer()
        .createCredentialDefinition(
          schema.name,
          schema: anoncreds.Schema,
          issuer_id,
          tag,
          signature_type,
          anoncreds.CredentialDefinitionConfig(supportRevocation)
        )

    CreateCredentialDefinition(
      credentialDefinition.getCredentialDefinition(),
      credentialDefinition.getCredentialDefinitionPrivate(),
      credentialDefinition.getCredentialKeyCorrectnessProof()
    )
  }

  // issuer
  def createOffer(
      credentialDefinition: CreateCredentialDefinition,
      credentialDefinitionId: String
  ): CredentialOffer =
    anoncreds
      .Issuer()
      .createCredentialOffer(
        credentialDefinition.cd.schemaId, // string schema_id,
        credentialDefinitionId, // string cred_def_id,
        credentialDefinition.proofKey // CredentialKeyCorrectnessProof correctness_proof
      )

  // holder
  def createCredentialRequest(
      linkSecret: LinkSecretWithId,
      credentialDefinition: CredentialDefinition,
      credentialOffer: CredentialOffer,
      entropy: String = {
        val tmp = scala.util.Random()
        tmp.setSeed(java.security.SecureRandom.getInstanceStrong().nextLong())
        tmp.nextString(80)
      }
  ): CreateCrendentialRequest = {
    val credentialRequest =
      anoncreds
        .Prover()
        .createCredentialRequest(
          entropy, // string? entropy,
          null, // string? prover_did,
          credentialDefinition, // CredentialDefinition cred_def,
          linkSecret.secret, // LinkSecret link_secret,
          linkSecret.id, // string link_secret_id,
          credentialOffer, // CredentialOffer credential_offer
        )

    CreateCrendentialRequest(credentialRequest.getRequest(), credentialRequest.getMetadata())
  }

  // holder
  def processCredential(
      credential: Credential,
      metadata: CredentialRequestMetadata,
      linkSecret: LinkSecretWithId,
      credentialDefinition: CredentialDefinition,
  ): Unit = {
    anoncreds
      .Prover()
      .processCredential(
        credential,
        metadata,
        linkSecret.secret,
        credentialDefinition,
        null
      )
  }

  // issuer
  def createCredential(
      credentialDefinition: CredentialDefinition,
      credentialDefinitionPrivate: CredentialDefinitionPrivate,
      credentialOffer: CredentialOffer,
      credentialRequest: CredentialRequest,
      attributeValues: Seq[(String, String)]
      //  java.util.List[AttributeValues] : java.util.List[AttributeValues]
      //  revocationRegistryId : String
      //  revocationStatusList : RevocationStatusList
      //  credentialRevocationConfig : CredentialRevocationConfig
  ): Credential = {

    anoncreds
      .Issuer()
      .createCredential(
        credentialDefinition, // CredentialDefinition cred_def,
        credentialDefinitionPrivate, // CredentialDefinitionPrivate cred_def_private,
        credentialOffer, // CredentialOffer cred_offer,
        credentialRequest, // CredentialRequest cred_request,
        attributeValues
          .map(e => anoncreds.AttributeValues(e._1, e._2))
          .asJava, // sequence<AttributeValues> cred_values,
        null, // RevocationRegistryId? rev_reg_id,
        null, // RevocationStatusList? rev_status_list,
        null, // CredentialRevocationConfig? revocation_config
      )
  }
}
