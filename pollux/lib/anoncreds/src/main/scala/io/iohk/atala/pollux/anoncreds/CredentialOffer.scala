package io.iohk.atala.pollux.anoncreds

/** The AnonCreds issuance process begins with the issuer constructing and sending a Credential Offer to the potential
  * holder. The Credential Offer contains the following JSON elements:
  *
  * { "schema_id": string, "cred_def_id": string, "nonce": string, "key_correctness_proof" : <key_correctness_proof> }
  * schema_id: The ID of the Schema on which the Public Credential Definition for the offered Credential is based.
  * cred_def_id: The ID of the Public Credential Definition on which the Credential to be issued will be based. nonce: A
  * random number generated for one time use by the issuer for preventing replay attacks and authentication between
  * protocol steps. The nonce must be present in the subsequent Credential Request from the holder.
  * key_correctness_proof: The Fiat-Shamir transformation challenge value in the non-interactive mode of Schnorr
  * Protocol. It is calculated by the issuer as the proof of knowledge of the private key used to create the Credential
  * Definition. This is verified by the holder during the creation of Credential Request.
  */
final case class CredentialOffer2() // FIXME
object CredentialOffer2 {}
