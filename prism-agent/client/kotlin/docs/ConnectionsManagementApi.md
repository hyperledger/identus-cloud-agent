# ConnectionsManagementApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**acceptConnectionInvitation**](ConnectionsManagementApi.md#acceptConnectionInvitation) | **POST** /connection-invitations | Accepts an Out of Band invitation.
[**createConnection**](ConnectionsManagementApi.md#createConnection) | **POST** /connections | Creates a new connection record and returns an Out of Band invitation.
[**getConnection**](ConnectionsManagementApi.md#getConnection) | **GET** /connections/{connectionId} | Gets an existing connection record by its unique identifier.
[**getConnections**](ConnectionsManagementApi.md#getConnections) | **GET** /connections | Gets the list of connection records.


<a id="acceptConnectionInvitation"></a>
# **acceptConnectionInvitation**
> Connection acceptConnectionInvitation(acceptConnectionInvitationRequest)

Accepts an Out of Band invitation.

 Accepts an [Out of Band 2.0](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) invitation, generates a new Peer DID, and submits a Connection Request to the inviter. It returns a connection object in &#x60;ConnectionRequestPending&#x60; state, until the Connection Request is eventually sent to the inviter by the prism-agent&#39;s background process. The connection object state will then automatically move to &#x60;ConnectionRequestSent&#x60;. 

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = ConnectionsManagementApi()
val acceptConnectionInvitationRequest : AcceptConnectionInvitationRequest =  // AcceptConnectionInvitationRequest | The request used by an invitee to accept a connection invitation received from an inviter, using out-of-band mechanism.
try {
    val result : Connection = apiInstance.acceptConnectionInvitation(acceptConnectionInvitationRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ConnectionsManagementApi#acceptConnectionInvitation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConnectionsManagementApi#acceptConnectionInvitation")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **acceptConnectionInvitationRequest** | [**AcceptConnectionInvitationRequest**](AcceptConnectionInvitationRequest.md)| The request used by an invitee to accept a connection invitation received from an inviter, using out-of-band mechanism. |

### Return type

[**Connection**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createConnection"></a>
# **createConnection**
> Connection createConnection(createConnectionRequest)

Creates a new connection record and returns an Out of Band invitation.

 Generates a new Peer DID and creates an [Out of Band 2.0](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) invitation. It returns a new connection record in &#x60;InvitationGenerated&#x60; state. The request body may contain a &#x60;label&#x60; that can be used as a human readable alias for the connection, for example &#x60;{&#39;label&#39;: \&quot;Bob\&quot;}&#x60; 

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = ConnectionsManagementApi()
val createConnectionRequest : CreateConnectionRequest =  // CreateConnectionRequest | JSON object required for the connection creation
try {
    val result : Connection = apiInstance.createConnection(createConnectionRequest)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ConnectionsManagementApi#createConnection")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConnectionsManagementApi#createConnection")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **createConnectionRequest** | [**CreateConnectionRequest**](CreateConnectionRequest.md)| JSON object required for the connection creation |

### Return type

[**Connection**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getConnection"></a>
# **getConnection**
> Connection getConnection(connectionId)

Gets an existing connection record by its unique identifier.

Gets an existing connection record by its unique identifier

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = ConnectionsManagementApi()
val connectionId : java.util.UUID = 38400000-8cf0-11bd-b23e-10b96e4ef00d // java.util.UUID | The unique identifier of the connection record.
try {
    val result : Connection = apiInstance.getConnection(connectionId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ConnectionsManagementApi#getConnection")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConnectionsManagementApi#getConnection")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **connectionId** | **java.util.UUID**| The unique identifier of the connection record. |

### Return type

[**Connection**](Connection.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getConnections"></a>
# **getConnections**
> ConnectionsPage getConnections(offset, limit)

Gets the list of connection records.

Get the list of connection records paginated

### Example
```kotlin
// Import classes:
//import io.iohk.atala.prism.infrastructure.*
//import io.iohk.atala.prism.models.*

val apiInstance = ConnectionsManagementApi()
val offset : kotlin.Int = 56 // kotlin.Int | 
val limit : kotlin.Int = 56 // kotlin.Int | 
try {
    val result : ConnectionsPage = apiInstance.getConnections(offset, limit)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ConnectionsManagementApi#getConnections")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ConnectionsManagementApi#getConnections")
    e.printStackTrace()
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **offset** | **kotlin.Int**|  | [optional]
 **limit** | **kotlin.Int**|  | [optional]

### Return type

[**ConnectionsPage**](ConnectionsPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

