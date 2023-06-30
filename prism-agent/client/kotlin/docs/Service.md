
# Service

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **kotlin.String** | The id of the service. Requires a URI fragment when use in create / update DID. Returns the full ID (with DID prefix) when resolving DID | 
**type** | **kotlin.String** | Service type. Can contain multiple possible values as described in the [Create DID operation](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#create-did) under the construction section. | 
**serviceEndpoint** | **kotlin.collections.List&lt;kotlin.String&gt;** | The service endpoint. Can contain multiple possible values as described in the [Create DID operation] | 



