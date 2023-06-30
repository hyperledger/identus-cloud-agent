# VerificationApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createVerificationPolicy**](VerificationApi.md#createVerificationPolicy) | **POST** /verification/policies | Create the new verification policy
[**deleteVerificationPolicyById**](VerificationApi.md#deleteVerificationPolicyById) | **DELETE** /verification/policies/{id} | Deleted the verification policy by id
[**getVerificationPolicyById**](VerificationApi.md#getVerificationPolicyById) | **GET** /verification/policies/{id} | Fetch the verification policy by id
[**lookupVerificationPoliciesByQuery**](VerificationApi.md#lookupVerificationPoliciesByQuery) | **GET** /verification/policies | Lookup verification policies by query
[**updateVerificationPolicy**](VerificationApi.md#updateVerificationPolicy) | **PUT** /verification/policies/{id} | Update the verification policy object by id


<a id="createVerificationPolicy"></a>
# **createVerificationPolicy**
> VerificationPolicy createVerificationPolicy(verificationPolicyInput)

Create the new verification policy

Create the new verification policy

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = VerificationApi()
val verificationPolicyInput : VerificationPolicyInput =  // VerificationPolicyInput | Create verification policy object
try {
    val result : VerificationPolicy = apiInstance.createVerificationPolicy(verificationPolicyInput)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling VerificationApi#createVerificationPolicy")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling VerificationApi#createVerificationPolicy")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **verificationPolicyInput** | [**VerificationPolicyInput**](VerificationPolicyInput.md)| Create verification policy object |

### Return type

[**VerificationPolicy**](VerificationPolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteVerificationPolicyById"></a>
# **deleteVerificationPolicyById**
> deleteVerificationPolicyById(id, nonce)

Deleted the verification policy by id

Delete the verification policy by id

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = VerificationApi()
val id : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | Delete the verification policy by id
val nonce : kotlin.Int = 56 // kotlin.Int | Nonce of the previous VerificationPolicy
try {
    apiInstance.deleteVerificationPolicyById(id, nonce)
} catch (e: ClientException) {
    println("4xx response calling VerificationApi#deleteVerificationPolicyById")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling VerificationApi#deleteVerificationPolicyById")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **java.util.UUID**| Delete the verification policy by id |
 **nonce** | **kotlin.Int**| Nonce of the previous VerificationPolicy |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getVerificationPolicyById"></a>
# **getVerificationPolicyById**
> VerificationPolicy getVerificationPolicyById(id)

Fetch the verification policy by id

Get the verification policy by id

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = VerificationApi()
val id : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | Get the verification policy by id
try {
    val result : VerificationPolicy = apiInstance.getVerificationPolicyById(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling VerificationApi#getVerificationPolicyById")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling VerificationApi#getVerificationPolicyById")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **java.util.UUID**| Get the verification policy by id |

### Return type

[**VerificationPolicy**](VerificationPolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="lookupVerificationPoliciesByQuery"></a>
# **lookupVerificationPoliciesByQuery**
> VerificationPolicyPage lookupVerificationPoliciesByQuery(name, offset, limit, order)

Lookup verification policies by query

Lookup verification policies by &#x60;name&#x60;, and control the pagination by &#x60;offset&#x60; and &#x60;limit&#x60; parameters

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = VerificationApi()
val name : kotlin.String = name_example // kotlin.String | 
val offset : kotlin.Int = 56 // kotlin.Int | 
val limit : kotlin.Int = 56 // kotlin.Int | 
val order : kotlin.String = order_example // kotlin.String | 
try {
    val result : VerificationPolicyPage = apiInstance.lookupVerificationPoliciesByQuery(name, offset, limit, order)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling VerificationApi#lookupVerificationPoliciesByQuery")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling VerificationApi#lookupVerificationPoliciesByQuery")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **kotlin.String**|  | [optional]
 **offset** | **kotlin.Int**|  | [optional]
 **limit** | **kotlin.Int**|  | [optional]
 **order** | **kotlin.String**|  | [optional]

### Return type

[**VerificationPolicyPage**](VerificationPolicyPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="updateVerificationPolicy"></a>
# **updateVerificationPolicy**
> VerificationPolicy updateVerificationPolicy(id, nonce, verificationPolicyInput)

Update the verification policy object by id

Update the verification policy entry

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = VerificationApi()
val id : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | 
val nonce : kotlin.Int = 56 // kotlin.Int | Nonce of the previous VerificationPolicy
val verificationPolicyInput : VerificationPolicyInput =  // VerificationPolicyInput | Update verification policy object
try {
    val result : VerificationPolicy = apiInstance.updateVerificationPolicy(id, nonce, verificationPolicyInput)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling VerificationApi#updateVerificationPolicy")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling VerificationApi#updateVerificationPolicy")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **java.util.UUID**|  |
 **nonce** | **kotlin.Int**| Nonce of the previous VerificationPolicy |
 **verificationPolicyInput** | [**VerificationPolicyInput**](VerificationPolicyInput.md)| Update verification policy object |

### Return type

[**VerificationPolicy**](VerificationPolicy.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

