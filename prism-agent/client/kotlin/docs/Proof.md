
# Proof

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | **kotlin.String** | The type of cryptographic signature algorithm used to generate the proof. | 
**created** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | The date and time at which the proof was created, in UTC format. This field is used to ensure that the proof was generated before or at the same time as the credential schema itself. | 
**verificationMethod** | **kotlin.String** | The verification method used to generate the proof. This is usually a DID and key ID combination that can be used to look up the public key needed to verify the proof. | 
**proofPurpose** | **kotlin.String** | The purpose of the proof (for example: &#x60;assertionMethod&#x60;). This indicates that the proof is being used to assert that the issuer really issued this credential schema instance. | 
**proofValue** | **kotlin.String** | The cryptographic signature value that was generated using the private key associated with the verification method, and which can be used to verify the proof. | 
**jws** | **kotlin.String** | The JSON Web Signature (JWS) that contains the proof information. | 
**domain** | **kotlin.String** | It specifies the domain context within which the credential schema and proof are being used |  [optional]



