# JWT credential revocation

Identus implements the revocation mechanism of JWT credentials according to [Bitstring Status List v1.0](https://www.w3.org/TR/2023/WD-vc-status-list-20230427/). This open standard enables Identus to verify the revocation status of any credential that implements the revocation mechanism using the same specification.

## Overview

Every credential will contain the property `credentialStatus`, which will look like this:

```json
"credentialStatus": {
    "id": "http://localhost:8080/cloud-agent/cloud-agent/credential-status/27526236-3836-4061-9867-f69314e258b4#94567"
    "type": "StatusList2021Entry",
    "statusPurpose": "revocation",
    "statusListIndex": "94567",
    "statusListCredential": "http://localhost:8080/cloud-agent/cloud-agent/credential-status/27526236-3836-4061-9867-f69314e258b4"
  },
```

* `type` will always be `StatusList2021Entry`
* `statusListCredential` is a publically accessible URL that resolves a status list credential that looks like this:
```json
{
  "proof" : {
    "type" : "DataIntegrityProof",
    "proofPurpose" : "assertionMethod",
    "verificationMethod" : "data:application/json;base64,eyJAY29udGV4dCI6WyJodHRwczovL3czaWQub3JnL3NlY3VyaXR5L211bHRpa2V5L3YxIl0sInR5cGUiOiJNdWx0aWtleSIsInB1YmxpY0tleU11bHRpYmFzZSI6InVNRll3RUFZSEtvWkl6ajBDQVFZRks0RUVBQW9EUWdBRUNYSUZsMlIxOGFtZUxELXlrU09HS1FvQ0JWYkZNNW91bGtjMnZJckp0UzRQWkJnMkxyNEQzUFdYR2xHTXB1aHdwSk84MEFpdzFXeVVHT1hONkJqSlFBPT0ifQ==",
    "created" : "2024-03-23T16:45:50.924279Z",
    "proofValue" : "ziKx1CJPKLy4U9kMmVzYct5xztq4oHRLPgMpAjh95zQxzBZorhLFmhZ85UPixJoQbaqkVaygLBnLARyxgGJGFNKFggaPSXHgJuG",
    "cryptoSuite" : "eddsa-jcs-2022"
  },
  "@context" : [
    "https://www.w3.org/2018/credentials/v1",
    "https://w3id.org/vc/status-list/2021/v1"
  ],
  "type" : [
    "VerifiableCredential",
    "StatusList2021Credential"
  ],
  "id" : "http://localhost:8080/cloud-agent/credential-status/27526236-3836-4061-9867-f69314e258b4",
  "issuer" : "did:prism:462c4811bf61d7de25b3baf86c5d2f0609b4debe53792d297bf612269bf8593a",
  "issuanceDate" : 1711212350,
  "credentialSubject" : {
    "id" : "",
    "type" : "StatusList2021",
    "statusPurpose" : "Revocation",
    "encodedList" : "H4sIAAAAAAAAAO3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"
  }
}

```
* `statusListIndex` is an index in a bit string at which the credential's revocation status can be verified.


The status list credential contains  `encodedList`, a base64-encoded bit string that contains the credential's revocation status.

## Verification

To verify the revocation status of the credential, one must follow these steps:

1. Resolve the Status list credential using the URL found at path: `credentialStatus.statusListCredential`
2. Verify the embedded proof of the credential.
3. Decode bit-string, which is in the JSON document of the Status list credential, found at path - `credentialSubject.encodedList`
4. Use the status list index from `credentialStatus.statusListIndex` to check if the bit at this index in the decoded bit-string from step 3 is on or off. If the bit is on, the credential is revoked. Otherwise, a revocation has yet to occur.

## Proof verification

Status list credential integrity can be verified using the embedded proof of type `DataIntegrityProof` via crypto suite `eddsa-jcs-2022`. The exact steps are in the [Data Integrity EdDSA Cryptosuites v1.0](https://www.w3.org/TR/vc-di-eddsa/#eddsa-jcs-2022)


## Revocation

Only issuers of a credential can revoke a credential.

*Get the list of credentials*
```bash
curl -X 'GET' \
  'http://localhost:8080/cloud-agent/issue-credentials/records' \
  -H 'accept: application/json'
```
This endpoint will return the credentials issued. Every credential includes an ID.

*Revoke the credential*
```bash
curl -X 'PATCH' \
  'http://localhost:8080/cloud-agent/revoke-credential/<credential_id>' \
  -H 'accept: */*'
```

:::note
[Present proof](./issue.md) will fail the verification if one of the credentials the holder presents a revoked credential.
:::