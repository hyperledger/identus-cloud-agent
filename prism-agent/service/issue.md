### Running multiple instances of Prism Agent
---

#### Starting an instance for `Issuer` on port `8080`

```bash
# From the root directory
PORT=8080 docker-compose -p issuer -f infrastructure/local/docker-compose.yml up
```

#### Starting an instance for `Holder` on port `8090`

```bash
# From the root directory
PORT=8090 docker-compose -p holder -f infrastructure/local/docker-compose.yml up
```

### Executing the `Issue` flow
---

- **Issuer** - Create a DID that will be used for issuing a VC with at least 1 `assertionMethod` key

```bash
curl --location --request POST 'http://localhost:8080/prism-agent/did-registrar/dids' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --data-raw '{
    "documentTemplate": {
      "publicKeys": [
        {
          "id": "my-issuing-key",
          "purpose": "assertionMethod"
        }
      ],
      "services": []
    }
  }'
```

- **Issuer** - Publish an issuing DID to the blockchain

Replace `DID_REF` by the DID on Prism Agent that should be published
```bash
curl --location --request POST 'http://localhost:8080/prism-agent/did-registrar/dids/{DID_REF}/publications' \
--header 'Accept: application/json'
```

- **Holder** - Create a Prism DID to receive a credential

Holder also needs a Prism DID to be used as a VC subject, but it is not required to be published.
The holder DID must have at least 1 `authentication` key for presenting credentials later in the process.

```bash
curl --location --request POST 'http://localhost:8090/prism-agent/did-registrar/dids' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --data-raw '{
    "documentTemplate": {
      "publicKeys": [
        {
          "id": "my-auth-key",
          "purpose": "authentication"
        }
      ],
      "services": []
    }
  }'
```

- **Issuer** - Initiate a new issue credential flow

Replace `{SUBJECT_ID}` with the DID of the holder and `{CONNECTION_ID}` with the connection to the holder.
This assumes that there is a connection already established (see ["connect" documentation](./connect.md)). Also `{ISSUING_DID}` must be specified using the DID created above.


```bash
curl -X 'POST' \
  'http://localhost:8080/prism-agent/issue-credentials/credential-offers' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
      "schemaId": "schema:1234",
      "subjectId": "{SUBJECT_ID}",
      "connectionId": "{CONNECTION_ID}",
      "issuingDID": "{ISSUING_DID}",
      "validityPeriod": 3600,
      "automaticIssuance": false,
      "claims": {
        "firstname": "Alice",
        "lastname": "Wonderland",
        "birthdate": "01/01/2000"
      }
 }' | jq
```

- **Holder** - Retrieving the list of issue records
```bash
curl -X 'GET' 'http://localhost:8090/prism-agent/issue-credentials/records' | jq
```

- **Holder** - Accepting the credential offer

Replace `{RECORD_ID}` with the UUID of the record from the previous list
```bash
curl -X 'POST' 'http://localhost:8090/prism-agent/issue-credentials/records/{RECORD_ID}/accept-offer' | jq
```

- **Issuer** - Retrieving the list of issue records
```bash
curl -X 'GET' 'http://localhost:8080/prism-agent/issue-credentials/records' | jq
```

- **Issuer** - Issuing the credential

Replace `{RECORD_ID}` with the UUID of the record from the previous list
```bash
curl -X 'POST' 'http://localhost:8080/prism-agent/issue-credentials/records/{RECORD_ID}/issue-credential' | jq
```
