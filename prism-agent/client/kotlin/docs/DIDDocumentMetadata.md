
# DIDDocumentMetadata

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**deactivated** | **kotlin.Boolean** | If a DID has been deactivated, DID document metadata MUST include this property with the boolean value true. If a DID has not been deactivated, this property is OPTIONAL, but if included, MUST have the boolean value false. |  [optional]
**canonicalId** | **kotlin.String** |  A DID in canonical form. If a DID is in long form and has been published, DID document metadata MUST contain a &#x60;canonicalId&#x60;&#x60; property with the short form DID as its value. If a DID in short form or has not been published, DID document metadata MUST NOT contain a &#x60;canonicalId&#x60; property.  |  [optional]



