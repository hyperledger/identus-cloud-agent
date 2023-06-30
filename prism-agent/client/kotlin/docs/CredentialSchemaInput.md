
# CredentialSchemaInput

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **kotlin.String** | A human-readable name for the credential schema. A piece of Metadata. | 
**version** | **kotlin.String** | Denotes the revision of a given Credential Schema. It should follow semantic version convention to describe the impact of the schema evolution. | 
**type** | **kotlin.String** | This field resolves to a JSON schema with details about the schema metadata that applies to the schema. A piece of Metadata. | 
**schema** | [**kotlin.Any**](.md) | Valid JSON Schema where the Credential Schema data fields are defined. A piece of Metadata | 
**author** | **kotlin.String** | DID of the identity which authored the credential schema. A piece of Metadata. | 
**description** | **kotlin.String** | A human-readable description of the credential schema |  [optional]
**tags** | **kotlin.collections.List&lt;kotlin.String&gt;** | Tokens that allow to lookup and filter the credential schema records. |  [optional]



