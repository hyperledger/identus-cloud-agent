# DIDApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getDID**](DIDApi.md#getDID) | **GET** /dids/{didRef} | Resolve Prism DID to a W3C representation


<a id="getDID"></a>
# **getDID**
> DIDResolutionResult getDID(didRef)

Resolve Prism DID to a W3C representation

Resolve Prism DID to a W3C DID document representation. The response can be the [DID resolution result](https://w3c-ccg.github.io/did-resolution/#did-resolution-result) or [DID document representation](https://www.w3.org/TR/did-core/#representations) depending on the &#x60;Accept&#x60; request header. The response is implemented according to [resolver HTTP binding](https://w3c-ccg.github.io/did-resolution/#bindings-https) in the DID resolution spec. 

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = DIDApi()
val didRef : kotlin.String = did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff // kotlin.String | Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax)
try {
    val result : DIDResolutionResult = apiInstance.getDID(didRef)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DIDApi#getDID")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DIDApi#getDID")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **didRef** | **kotlin.String**| Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax) |

### Return type

[**DIDResolutionResult**](DIDResolutionResult.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/ld+json; profile=https://w3id.org/did-resolution, application/did+ld+json

