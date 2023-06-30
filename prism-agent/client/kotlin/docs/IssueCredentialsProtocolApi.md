# IssueCredentialsProtocolApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**acceptCredentialOffer**](IssueCredentialsProtocolApi.md#acceptCredentialOffer) | **POST** /issue-credentials/records/{recordId}/accept-offer | As a holder, accepts a credential offer received from an issuer.
[**createCredentialOffer**](IssueCredentialsProtocolApi.md#createCredentialOffer) | **POST** /issue-credentials/credential-offers | As a credential issuer, create a new credential offer to be sent to a holder.
[**getCredentialRecord**](IssueCredentialsProtocolApi.md#getCredentialRecord) | **GET** /issue-credentials/records/{recordId} | Gets an existing issue credential record by its unique identifier.
[**getCredentialRecords**](IssueCredentialsProtocolApi.md#getCredentialRecords) | **GET** /issue-credentials/records | Gets the list of issue credential records.
[**issueCredential**](IssueCredentialsProtocolApi.md#issueCredential) | **POST** /issue-credentials/records/{recordId}/issue-credential | As an issuer, issues the verifiable credential related to the specified record.


<a id="acceptCredentialOffer"></a>
# **acceptCredentialOffer**
> IssueCredentialRecord acceptCredentialOffer(recordId, acceptCredentialOfferRequest)

As a holder, accepts a credential offer received from an issuer.

Accepts a credential offer received from a VC issuer and sends back a credential request.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = IssueCredentialsProtocolApi()
val recordId : kotlin.String = recordId_example // kotlin.String | The unique identifier of the issue credential record.
val acceptCredentialOfferRequest : AcceptCredentialOfferRequest =  // AcceptCredentialOfferRequest | The accept credential offer request object.
try {
    val result : IssueCredentialRecord = apiInstance.acceptCredentialOffer(recordId, acceptCredentialOfferRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling IssueCredentialsProtocolApi#acceptCredentialOffer")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling IssueCredentialsProtocolApi#acceptCredentialOffer")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **recordId** | **kotlin.String**| The unique identifier of the issue credential record. |
 **acceptCredentialOfferRequest** | [**AcceptCredentialOfferRequest**](AcceptCredentialOfferRequest.md)| The accept credential offer request object. |

### Return type

[**IssueCredentialRecord**](IssueCredentialRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createCredentialOffer"></a>
# **createCredentialOffer**
> IssueCredentialRecord createCredentialOffer(createIssueCredentialRecordRequest)

As a credential issuer, create a new credential offer to be sent to a holder.

Creates a new credential offer in the database

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = IssueCredentialsProtocolApi()
val createIssueCredentialRecordRequest : CreateIssueCredentialRecordRequest =  // CreateIssueCredentialRecordRequest | The credential offer object.
try {
    val result : IssueCredentialRecord = apiInstance.createCredentialOffer(createIssueCredentialRecordRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling IssueCredentialsProtocolApi#createCredentialOffer")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling IssueCredentialsProtocolApi#createCredentialOffer")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **createIssueCredentialRecordRequest** | [**CreateIssueCredentialRecordRequest**](CreateIssueCredentialRecordRequest.md)| The credential offer object. |

### Return type

[**IssueCredentialRecord**](IssueCredentialRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getCredentialRecord"></a>
# **getCredentialRecord**
> IssueCredentialRecord getCredentialRecord(recordId)

Gets an existing issue credential record by its unique identifier.

Gets issue credential records by record id

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = IssueCredentialsProtocolApi()
val recordId : kotlin.String = recordId_example // kotlin.String | The unique identifier of the issue credential record.
try {
    val result : IssueCredentialRecord = apiInstance.getCredentialRecord(recordId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling IssueCredentialsProtocolApi#getCredentialRecord")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling IssueCredentialsProtocolApi#getCredentialRecord")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **recordId** | **kotlin.String**| The unique identifier of the issue credential record. |

### Return type

[**IssueCredentialRecord**](IssueCredentialRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getCredentialRecords"></a>
# **getCredentialRecords**
> IssueCredentialRecordPage getCredentialRecords(offset, limit, thid)

Gets the list of issue credential records.

Get the list of issue credential records paginated

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = IssueCredentialsProtocolApi()
val offset : kotlin.Int = 56 // kotlin.Int | 
val limit : kotlin.Int = 56 // kotlin.Int | 
val thid : kotlin.String = thid_example // kotlin.String | The thid of a DIDComm communication.
try {
    val result : IssueCredentialRecordPage = apiInstance.getCredentialRecords(offset, limit, thid)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling IssueCredentialsProtocolApi#getCredentialRecords")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling IssueCredentialsProtocolApi#getCredentialRecords")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **offset** | **kotlin.Int**|  | [optional]
 **limit** | **kotlin.Int**|  | [optional]
 **thid** | **kotlin.String**| The thid of a DIDComm communication. | [optional]

### Return type

[**IssueCredentialRecordPage**](IssueCredentialRecordPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="issueCredential"></a>
# **issueCredential**
> IssueCredentialRecord issueCredential(recordId)

As an issuer, issues the verifiable credential related to the specified record.

Sends credential to a holder (holder DID is specified in credential as subjectDid). Credential is constructed from the credential records found by credential id.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = IssueCredentialsProtocolApi()
val recordId : kotlin.String = recordId_example // kotlin.String | The unique identifier of the issue credential record.
try {
    val result : IssueCredentialRecord = apiInstance.issueCredential(recordId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling IssueCredentialsProtocolApi#issueCredential")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling IssueCredentialsProtocolApi#issueCredential")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **recordId** | **kotlin.String**| The unique identifier of the issue credential record. |

### Return type

[**IssueCredentialRecord**](IssueCredentialRecord.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

