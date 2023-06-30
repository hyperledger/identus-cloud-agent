# DIDRegistrarApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getDidRegistrarDids**](DIDRegistrarApi.md#getDidRegistrarDids) | **GET** /did-registrar/dids | List all DIDs stored in Prism Agent&#39;s wallet
[**getDidRegistrarDidsDidref**](DIDRegistrarApi.md#getDidRegistrarDidsDidref) | **GET** /did-registrar/dids/{didRef} | Get DID stored in Prism Agent&#39;s wallet
[**postDidRegistrarDids**](DIDRegistrarApi.md#postDidRegistrarDids) | **POST** /did-registrar/dids | Create unpublished DID and store it in Prism Agent&#39;s wallet
[**postDidRegistrarDidsDidrefDeactivations**](DIDRegistrarApi.md#postDidRegistrarDidsDidrefDeactivations) | **POST** /did-registrar/dids/{didRef}/deactivations | Deactivate DID in Prism Agent&#39;s wallet and post deactivate operation to the VDR
[**postDidRegistrarDidsDidrefPublications**](DIDRegistrarApi.md#postDidRegistrarDidsDidrefPublications) | **POST** /did-registrar/dids/{didRef}/publications | Publish the DID stored in Prism Agent&#39;s wallet to the VDR
[**postDidRegistrarDidsDidrefUpdates**](DIDRegistrarApi.md#postDidRegistrarDidsDidrefUpdates) | **POST** /did-registrar/dids/{didRef}/updates | Update DID in Prism Agent&#39;s wallet and post update operation to the VDR


<a id="getDidRegistrarDids"></a>
# **getDidRegistrarDids**
> ManagedDIDPage getDidRegistrarDids(offset, limit)

List all DIDs stored in Prism Agent&#39;s wallet

List all DIDs stored in Prism Agent&#39;s wallet. Return a paginated items ordered by created timestamp. If the &#x60;limit&#x60; parameter is not set, it defaults to 100 items per page.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = DIDRegistrarApi()
val offset : kotlin.Int = 56 // kotlin.Int | 
val limit : kotlin.Int = 56 // kotlin.Int | 
try {
    val result : ManagedDIDPage = apiInstance.getDidRegistrarDids(offset, limit)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DIDRegistrarApi#getDidRegistrarDids")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DIDRegistrarApi#getDidRegistrarDids")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **offset** | **kotlin.Int**|  | [optional]
 **limit** | **kotlin.Int**|  | [optional]

### Return type

[**ManagedDIDPage**](ManagedDIDPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getDidRegistrarDidsDidref"></a>
# **getDidRegistrarDidsDidref**
> ManagedDID getDidRegistrarDidsDidref(didRef)

Get DID stored in Prism Agent&#39;s wallet

Get DID stored in Prism Agent&#39;s wallet

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = DIDRegistrarApi()
val didRef : kotlin.String = did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff // kotlin.String | Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax)
try {
    val result : ManagedDID = apiInstance.getDidRegistrarDidsDidref(didRef)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DIDRegistrarApi#getDidRegistrarDidsDidref")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DIDRegistrarApi#getDidRegistrarDidsDidref")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **didRef** | **kotlin.String**| Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax) |

### Return type

[**ManagedDID**](ManagedDID.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="postDidRegistrarDids"></a>
# **postDidRegistrarDids**
> CreateManagedDIDResponse postDidRegistrarDids(createManagedDidRequest)

Create unpublished DID and store it in Prism Agent&#39;s wallet

Create unpublished DID and store it inside Prism Agent&#39;s wallet. The private keys of the DID is managed by Prism Agent. The DID can later be published to the VDR using publications endpoint.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = DIDRegistrarApi()
val createManagedDidRequest : CreateManagedDidRequest =  // CreateManagedDidRequest | 
try {
    val result : CreateManagedDIDResponse = apiInstance.postDidRegistrarDids(createManagedDidRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DIDRegistrarApi#postDidRegistrarDids")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DIDRegistrarApi#postDidRegistrarDids")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **createManagedDidRequest** | [**CreateManagedDidRequest**](CreateManagedDidRequest.md)|  |

### Return type

[**CreateManagedDIDResponse**](CreateManagedDIDResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="postDidRegistrarDidsDidrefDeactivations"></a>
# **postDidRegistrarDidsDidrefDeactivations**
> DIDOperationResponse postDidRegistrarDidsDidrefDeactivations(didRef)

Deactivate DID in Prism Agent&#39;s wallet and post deactivate operation to the VDR

Deactivate DID in Prism Agent&#39;s wallet and post deactivate operation to the VDR.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = DIDRegistrarApi()
val didRef : kotlin.String = did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff // kotlin.String | Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax)
try {
    val result : DIDOperationResponse = apiInstance.postDidRegistrarDidsDidrefDeactivations(didRef)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DIDRegistrarApi#postDidRegistrarDidsDidrefDeactivations")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DIDRegistrarApi#postDidRegistrarDidsDidrefDeactivations")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **didRef** | **kotlin.String**| Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax) |

### Return type

[**DIDOperationResponse**](DIDOperationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="postDidRegistrarDidsDidrefPublications"></a>
# **postDidRegistrarDidsDidrefPublications**
> DIDOperationResponse postDidRegistrarDidsDidrefPublications(didRef)

Publish the DID stored in Prism Agent&#39;s wallet to the VDR

Publish the DID stored in Prism Agent&#39;s wallet to the VDR.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = DIDRegistrarApi()
val didRef : kotlin.String = did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff // kotlin.String | Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax)
try {
    val result : DIDOperationResponse = apiInstance.postDidRegistrarDidsDidrefPublications(didRef)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DIDRegistrarApi#postDidRegistrarDidsDidrefPublications")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DIDRegistrarApi#postDidRegistrarDidsDidrefPublications")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **didRef** | **kotlin.String**| Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax) |

### Return type

[**DIDOperationResponse**](DIDOperationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="postDidRegistrarDidsDidrefUpdates"></a>
# **postDidRegistrarDidsDidrefUpdates**
> DIDOperationResponse postDidRegistrarDidsDidrefUpdates(didRef, updateManagedDIDRequest)

Update DID in Prism Agent&#39;s wallet and post update operation to the VDR

Update DID in Prism Agent&#39;s wallet and post update operation to the VDR. This endpoint updates the DID document from the last confirmed operation. Submitting multiple update operations without waiting for confirmation will result in some operations being rejected as only one operation is allowed to be appended to the last confirmed operation.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = DIDRegistrarApi()
val didRef : kotlin.String = did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff // kotlin.String | Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax)
val updateManagedDIDRequest : UpdateManagedDIDRequest =  // UpdateManagedDIDRequest | 
try {
    val result : DIDOperationResponse = apiInstance.postDidRegistrarDidsDidrefUpdates(didRef, updateManagedDIDRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling DIDRegistrarApi#postDidRegistrarDidsDidrefUpdates")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling DIDRegistrarApi#postDidRegistrarDidsDidrefUpdates")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **didRef** | **kotlin.String**| Prism DID according to [the Prism DID method syntax](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#prism-did-method-syntax) |
 **updateManagedDIDRequest** | [**UpdateManagedDIDRequest**](UpdateManagedDIDRequest.md)|  |

### Return type

[**DIDOperationResponse**](DIDOperationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

