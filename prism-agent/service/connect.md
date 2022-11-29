### Running multiple instances of Prism Agent
---

#### Starting an instance for `Inviter` on port `8080`

```bash
# From the root directory
PORT=8080 docker-compose -p inviter -f infrastructure/local/docker-compose.yml up
```

#### Starting an instance for `Invitee` on port `8090`

```bash
# From the root directory
PORT=8090 docker-compose -p invitee -f infrastructure/local/docker-compose.yml up
```

### Executing the `Connect` flow
---

- **Inviter** - Create a connection record containing the invitation
```bash
curl -X 'POST' \
	'http://localhost:8080/prism-agent/connections' \
	-H 'Content-Type: application/json' \
	-d '{
		"label": "Connect with Alice"
		}' | jq
```

- **Inviter** - Retrieving the list of connections
```bash
curl -X 'GET' 'http://localhost:8080/prism-agent/connections' | jq
```

- **Invitee** - Accept OOB invitation

Replace `{RAW_INVITATION}` with the value of the '_oob' query string parameter from the invitation URL above
```bash
curl -X 'POST' \
	'http://localhost:8090/prism-agent/connection-invitations' \
	-H 'Content-Type: application/json' \
	-d '{
		"invitation": "{RAW_INVITATION}"
	}' | jq
```

- **Invitee** - Retrieving the list of connections
```bash
curl -X 'GET' 'http://localhost:8090/prism-agent/connections' | jq
```
