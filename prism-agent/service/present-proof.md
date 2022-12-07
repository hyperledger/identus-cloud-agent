# TODO

connectionId is the holder (connectionId or did)

```shell
curl -X 'POST' \
  'http://localhost:8070/prism-agent/present-proof/presentations' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "connectionId": "did:peer:2.Ez6LSjFWB93ToXe9U1BVfxPSduu8j8rEpshjjtsjthhkjHk2S.Vz6MkhpRLxVTvKPLjUB23Hk4u9m3oYkCxoKFwSj5tqgxc9dtR.SeyJ0IjoiZG0iLCJzIjoiaHR0cDovL2hvc3QuZG9ja2VyLmludGVybmFsOjgwOTAvZGlkY29tbS8iLCJyIjpbXSwiYSI6WyJkaWRjb21tL3YyIl19", "proofs": [{"schemaId": "schema:1234", "trustIssuers":[]}]
}'
```


get the presentationId
```shell
curl -X 'GET' 'http://localhost:8090/prism-agent/present-proof/presentations' -H 'accept: application/json' | jq
```



get the "recordId": "b87975df-57c5-4fe4-a668-1d78aa9497df"
```shell
curl -X 'GET' 'http://localhost:8090/prism-agent/issue-credentials/records' -H 'accept: application/json' | jq
```



use the recordId and  presentationId
FIXME foound
```shell
curl -X 'PATCH' \
  'http://localhost:8090/prism-agent/present-proof/presentations/6c2d8419-9274-4e8b-9486-1a057ede202a' \
  -H 'Content-Type: application/json' \
  -d '{
  "action": "request-accept",
  "proofId": ["fd4aad76-b5d0-4afb-a8bd-466e2006fdba"]
}'
```

# check PresentationSent !
```shell
curl -X 'GET' 'http://localhost:8090/prism-agent/present-proof/presentations' -H 'accept: application/json' | jq
```

# check PresentationReceived !
```shell
curl -X 'GET' 'http://localhost:8070/prism-agent/present-proof/presentations' -H 'accept: application/json' | jq
```

```shell
curl -X 'PATCH' \
  'http://localhost:8070/prism-agent/present-proof/presentations/10c632fc-a8b5-4138-9a7e-b94f06b9663f' \
  -H 'Content-Type: application/json' \
  -d '{"action": "presentation-accept"}' | jq
```