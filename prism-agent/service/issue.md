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

- **Issuer** - Initiate a new issue credential flow

Replace `{SUBJECT_ID}` with the DID of the holder displayed at startup in the his Prism Agent console logs
```bash
curl -X 'POST' \
  'http://localhost:8080/prism-agent/issue-credentials/credential-offers' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
      "schemaId": "schema:1234",
      "subjectId": "{SUBJECT_ID}",
      "validityPeriod": 3600,
      "automaticIssuance": false,
      "awaitConfirmation": false,
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