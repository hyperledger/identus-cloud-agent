
# CredentialSchemaResponsePage

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**kind** | **kotlin.String** | A string field indicating the type of the API response. In this case, it will always be set to &#x60;CredentialSchemaPage&#x60; | 
**self** | **kotlin.String** | A string field containing the URL of the current API endpoint | 
**pageOf** | **kotlin.String** | A string field indicating the type of resource that the contents field contains | 
**contents** | [**kotlin.collections.List&lt;CredentialSchemaResponse&gt;**](CredentialSchemaResponse.md) | A sequence of CredentialSchemaResponse objects representing the list of credential schemas that the API response contains |  [optional]
**next** | **kotlin.String** | An optional string field containing the URL of the next page of results. If the API response does not contain any more pages, this field should be set to None. |  [optional]
**previous** | **kotlin.String** | An optional string field containing the URL of the previous page of results. If the API response is the first page of results, this field should be set to None. |  [optional]



