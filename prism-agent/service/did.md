### Running single instance of Prism Agent
---

#### Starting an instance on port `8080`

Follow the instruction on `./infrastructure/local/README.md` for local deployment.

```bash
./infrastructure/local/run.sh -p 8080
```

### Executing a simple DID lifecycle flow
---

- **Organization** creates a new unpublised DID stored in Prism Agent
```bash
curl --location --request POST 'http://localhost:8080/prism-agent/did-registrar/dids' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --data-raw '{
    "documentTemplate": {
      "publicKeys": [
        {
          "id": "auth0",
          "purpose": "authentication"
        }
      ],
      "services": []
    }
  }'
```

- **Organization** lists all the DIDs in Prism Agent
```bash
curl --location --request GET 'http://localhost:8080/prism-agent/did-registrar/dids' \
  --header 'Accept: application/json'
```

- **Organization** publishes the DID in Prism Agent to the blockchain
Replace `DID_REF` by the DID on Prism Agent that should be published
```bash
curl --location --request POST 'http://localhost:8080/prism-agent/did-registrar/dids/{DID_REF}/publications' \
--header 'Accept: application/json'
```
- **Organization** resolves the DID document of Prism DID
Replace `DID_REF` by the DID on Prism Agent that should be resolved
```bash
curl --location --request GET 'http://localhost:8080/prism-agent/dids/{DID_REF}' \
--header 'Accept: application/json'
```
