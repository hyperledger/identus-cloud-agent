# PresentProofApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllPresentation**](PresentProofApi.md#getAllPresentation) | **GET** /present-proof/presentations | Gets the list of proof presentation records.
[**getPresentation**](PresentProofApi.md#getPresentation) | **GET** /present-proof/presentations/{presentationId} | Gets an existing proof presentation record by its unique identifier. More information on the error can be found in the response body.
[**requestPresentation**](PresentProofApi.md#requestPresentation) | **POST** /present-proof/presentations | As a Verifier, create a new proof presentation request and send it to the Prover.
[**updatePresentation**](PresentProofApi.md#updatePresentation) | **PATCH** /present-proof/presentations/{presentationId} | Updates the proof presentation record matching the unique identifier, with the specific action to perform.


<a id="getAllPresentation"></a>
# **getAllPresentation**
> PresentationStatusPage getAllPresentation(offset, limit, thid)

Gets the list of proof presentation records.

list of presentation statuses

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = PresentProofApi()
val offset : kotlin.Int = 56 // kotlin.Int | 
val limit : kotlin.Int = 56 // kotlin.Int | 
val thid : kotlin.String = thid_example // kotlin.String | 
try {
    val result : PresentationStatusPage = apiInstance.getAllPresentation(offset, limit, thid)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PresentProofApi#getAllPresentation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PresentProofApi#getAllPresentation")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **offset** | **kotlin.Int**|  | [optional]
 **limit** | **kotlin.Int**|  | [optional]
 **thid** | **kotlin.String**|  | [optional]

### Return type

[**PresentationStatusPage**](PresentationStatusPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getPresentation"></a>
# **getPresentation**
> PresentationStatus getPresentation(presentationId)

Gets an existing proof presentation record by its unique identifier. More information on the error can be found in the response body.

Returns an existing presentation record by id.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = PresentProofApi()
val presentationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | The unique identifier of the presentation record.
try {
    val result : PresentationStatus = apiInstance.getPresentation(presentationId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PresentProofApi#getPresentation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PresentProofApi#getPresentation")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **presentationId** | **java.util.UUID**| The unique identifier of the presentation record. |

### Return type

[**PresentationStatus**](PresentationStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="requestPresentation"></a>
# **requestPresentation**
> RequestPresentationOutput requestPresentation(requestPresentationInput)

As a Verifier, create a new proof presentation request and send it to the Prover.

Holder presents proof derived from the verifiable credential to verifier.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = PresentProofApi()
val requestPresentationInput : RequestPresentationInput =  // RequestPresentationInput | The present proof creation request.
try {
    val result : RequestPresentationOutput = apiInstance.requestPresentation(requestPresentationInput)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PresentProofApi#requestPresentation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PresentProofApi#requestPresentation")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **requestPresentationInput** | [**RequestPresentationInput**](RequestPresentationInput.md)| The present proof creation request. |

### Return type

[**RequestPresentationOutput**](RequestPresentationOutput.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="updatePresentation"></a>
# **updatePresentation**
> PresentationStatus updatePresentation(presentationId, requestPresentationAction)

Updates the proof presentation record matching the unique identifier, with the specific action to perform.

Accept or reject presentation of proof request.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = PresentProofApi()
val presentationId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | The unique identifier of the presentation record.
val requestPresentationAction : RequestPresentationAction =  // RequestPresentationAction | The action to perform on the proof presentation record.
try {
    val result : PresentationStatus = apiInstance.updatePresentation(presentationId, requestPresentationAction)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PresentProofApi#updatePresentation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PresentProofApi#updatePresentation")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **presentationId** | **java.util.UUID**| The unique identifier of the presentation record. |
 **requestPresentationAction** | [**RequestPresentationAction**](RequestPresentationAction.md)| The action to perform on the proof presentation record. |

### Return type

[**PresentationStatus**](PresentationStatus.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

