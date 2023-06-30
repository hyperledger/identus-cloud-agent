# SchemaRegistryApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createSchema**](SchemaRegistryApi.md#createSchema) | **POST** /schema-registry/schemas | Publish new schema to the schema registry
[**getSchemaById**](SchemaRegistryApi.md#getSchemaById) | **GET** /schema-registry/schemas/{guid} | Fetch the schema from the registry by &#x60;guid&#x60;
[**lookupSchemasByQuery**](SchemaRegistryApi.md#lookupSchemasByQuery) | **GET** /schema-registry/schemas | Lookup schemas by indexed fields
[**test**](SchemaRegistryApi.md#test) | **GET** /schema-registry/test | Trace the request input from the point of view of the server
[**updateSchema**](SchemaRegistryApi.md#updateSchema) | **PUT** /schema-registry/{author}/{id} | Publish the new version of the credential schema to the schema registry


<a id="createSchema"></a>
# **createSchema**
> CredentialSchemaResponse createSchema(credentialSchemaInput)

Publish new schema to the schema registry

Create the new credential schema record with metadata and internal JSON Schema on behalf of Cloud Agent. The credential schema will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = SchemaRegistryApi()
val credentialSchemaInput : CredentialSchemaInput =  // CredentialSchemaInput | JSON object required for the credential schema creation
try {
    val result : CredentialSchemaResponse = apiInstance.createSchema(credentialSchemaInput)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SchemaRegistryApi#createSchema")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SchemaRegistryApi#createSchema")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **credentialSchemaInput** | [**CredentialSchemaInput**](CredentialSchemaInput.md)| JSON object required for the credential schema creation |

### Return type

[**CredentialSchemaResponse**](CredentialSchemaResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getSchemaById"></a>
# **getSchemaById**
> CredentialSchemaResponse getSchemaById(guid)

Fetch the schema from the registry by &#x60;guid&#x60;

Fetch the credential schema by the unique identifier

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = SchemaRegistryApi()
val guid : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | Globally unique identifier of the credential schema record
try {
    val result : CredentialSchemaResponse = apiInstance.getSchemaById(guid)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SchemaRegistryApi#getSchemaById")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SchemaRegistryApi#getSchemaById")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **guid** | **java.util.UUID**| Globally unique identifier of the credential schema record |

### Return type

[**CredentialSchemaResponse**](CredentialSchemaResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="lookupSchemasByQuery"></a>
# **lookupSchemasByQuery**
> CredentialSchemaResponsePage lookupSchemasByQuery(author, name, version, tags, offset, limit, order)

Lookup schemas by indexed fields

Lookup schemas by &#x60;author&#x60;, &#x60;name&#x60;, &#x60;tags&#x60; parameters and control the pagination by &#x60;offset&#x60; and &#x60;limit&#x60; parameters 

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = SchemaRegistryApi()
val author : kotlin.String = did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff // kotlin.String | 
val name : kotlin.String = DrivingLicense // kotlin.String | 
val version : kotlin.String = 1.0.0 // kotlin.String | 
val tags : kotlin.String = driving // kotlin.String | 
val offset : kotlin.Int = 56 // kotlin.Int | 
val limit : kotlin.Int = 56 // kotlin.Int | 
val order : kotlin.String = order_example // kotlin.String | 
try {
    val result : CredentialSchemaResponsePage = apiInstance.lookupSchemasByQuery(author, name, version, tags, offset, limit, order)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SchemaRegistryApi#lookupSchemasByQuery")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SchemaRegistryApi#lookupSchemasByQuery")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **author** | **kotlin.String**|  | [optional]
 **name** | **kotlin.String**|  | [optional]
 **version** | **kotlin.String**|  | [optional]
 **tags** | **kotlin.String**|  | [optional]
 **offset** | **kotlin.Int**|  | [optional]
 **limit** | **kotlin.Int**|  | [optional]
 **order** | **kotlin.String**|  | [optional]

### Return type

[**CredentialSchemaResponsePage**](CredentialSchemaResponsePage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="test"></a>
# **test**
> kotlin.String test()

Trace the request input from the point of view of the server

Trace the request input from the point of view of the server

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = SchemaRegistryApi()
try {
    val result : kotlin.String = apiInstance.test()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SchemaRegistryApi#test")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SchemaRegistryApi#test")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**kotlin.String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="updateSchema"></a>
# **updateSchema**
> CredentialSchemaResponse updateSchema(author, id, credentialSchemaInput)

Publish the new version of the credential schema to the schema registry

Publish the new version of the credential schema record with metadata and internal JSON Schema on behalf of Cloud Agent. The credential schema will be signed by the keys of Cloud Agent and issued by the DID that corresponds to it.

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = SchemaRegistryApi()
val author : kotlin.String = author_example // kotlin.String | DID of the identity which authored the credential schema. A piece of Metadata.
val id : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | A locally unique identifier to address the schema. UUID is generated by the backend.
val credentialSchemaInput : CredentialSchemaInput =  // CredentialSchemaInput | JSON object required for the credential schema update
try {
    val result : CredentialSchemaResponse = apiInstance.updateSchema(author, id, credentialSchemaInput)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SchemaRegistryApi#updateSchema")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SchemaRegistryApi#updateSchema")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **author** | **kotlin.String**| DID of the identity which authored the credential schema. A piece of Metadata. |
 **id** | **java.util.UUID**| A locally unique identifier to address the schema. UUID is generated by the backend. |
 **credentialSchemaInput** | [**CredentialSchemaInput**](CredentialSchemaInput.md)| JSON object required for the credential schema update |

### Return type

[**CredentialSchemaResponse**](CredentialSchemaResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

