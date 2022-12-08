## Follow Issue flow as documented below so the the holder has a credential

---
## Issue flow
Basic documentation on how to execute the Issue flow from the command line can be found [here](./issue.md).


### Running  instances of verifier Agent
---

#### Starting an instance for `Verifier` on port `8070`
### You can stop the `Issuer` if you are running out of resources locally

```bash
# From the root directory
PORT=8070 docker-compose -p verifier -f infrastructure/local/docker-compose.yml up
```

### Executing the `Verifier` flow
---
connectionId is the holder (connectionId or did)
Replace `{CONNECTION_ID}` with the DID of the holder displayed at startup in the his Prism Agent console logs

- **Verifier** - Initiates a Proof Request

```shell
curl -X 'POST' \
  'http://localhost:8070/prism-agent/present-proof/presentations' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "connectionId": "{CONNECTION_ID}", "proofs":[]
}'
```
- **Holder** - Retrieving the list of presentation records

```shell
curl -X 'GET' 'http://localhost:8090/prism-agent/present-proof/presentations' -H 'accept: application/json' | jq
```

- **Holder** - Retrieving the list of credentials records choose the `{RECORD_ID}` for credential with status CredentialRecieved 

```shell
curl -X 'GET' 'http://localhost:8090/prism-agent/issue-credentials/records' -H 'accept: application/json' | jq
```

- **Holder** - Accepting the Presentation Request 
Replace `{PRESENTATION_ID}` with the UUID of the record from the presentation records list
Replace `{RECORD_ID}` with the UUID of the record from the credential records list


```shell
curl -X 'PATCH' \
  'http://localhost:8090/prism-agent/present-proof/presentations/{PRESENTATION_ID}' \
  -H 'Content-Type: application/json' \
  -d '{
  "action": "request-accept",
  "proofId": ["{RECORD_ID}"]
}'
```
- **Holder** - check Presentation state  PresentationSent 
# check PresentationSent !
```shell
curl -X 'GET' 'http://localhost:8090/prism-agent/present-proof/presentations' -H 'accept: application/json' | jq
```

- **Verifier** - check Presentation state  PresentationReceived 
# check PresentationReceived !
```shell
curl -X 'GET' 'http://localhost:8070/prism-agent/present-proof/presentations' -H 'accept: application/json' | jq
```
- **Verifier** - Accept PresentationReceived 
Replace `{PRESENTATION_ID}` with the UUID of the record from the presentation records list with state PresentationReceived

```shell
curl -X 'PATCH' \
  'http://localhost:8070/prism-agent/present-proof/presentations/{PRESENTATION_ID}' \
  -H 'Content-Type: application/json' \
  -d '{"action": "presentation-accept"}' | jq
```

- **Verifier** - check Presentation state  PresentationVerified 
# check PresentationVerified !

```shell
curl -X 'GET' 'http://localhost:8070/prism-agent/present-proof/presentations' -H 'accept: application/json' | jq
```