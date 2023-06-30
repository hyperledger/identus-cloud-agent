
# ManagedDID

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**did** | **kotlin.String** | A managed DID | 
**status** | **kotlin.String** | A status indicating a publication state of a DID in the wallet (e.g. PUBLICATION_PENDING, PUBLISHED). Does not represent DID a full lifecyle (e.g. deactivated, recovered, updated). | 
**longFormDid** | **kotlin.String** | A long-form DID. Mandatory when status is not PUBLISHED and optional when status is PUBLISHED |  [optional]



