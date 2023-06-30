
# IssueCredentialRecordPage

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**self** | **kotlin.String** | A string field containing the URL of the current API endpoint | 
**kind** | **kotlin.String** | A string field containing the URL of the current API endpoint | 
**pageOf** | **kotlin.String** | A string field indicating the type of resource that the contents field contains | 
**next** | **kotlin.String** | An optional string field containing the URL of the next page of results. If the API response does not contain any more pages, this field should be set to None. |  [optional]
**previous** | **kotlin.String** | An optional string field containing the URL of the previous page of results. If the API response is the first page of results, this field should be set to None. |  [optional]
**contents** | [**kotlin.collections.List&lt;IssueCredentialRecord&gt;**](IssueCredentialRecord.md) | A sequence of IssueCredentialRecord objects representing the list of credential records that the API response contains |  [optional]



