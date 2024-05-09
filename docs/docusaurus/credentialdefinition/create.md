# Create the Credential Definition

The Cloud Agent exposes REST API for creation, fetching, and searching the [credential definition](/docs/concepts/glossary#credential-definition) records.

The OpenAPI specification and ReDoc documentation describe the endpoint.

In this document, you can find step-by-step instructions for creating the credential definition.

## Step-by-step guide

The following guide demonstrates how to create a birth certificate credential definition.

### 1. Define the Credential Definition for the Verifiable Credential

Assume you are aiming to define a credential for birth certificates. This credential definition has specific properties and ties to a schema in the Cloud Agent.

Here's a sample content of the credential definition:

```json
{
  "name": "Birth Certificate location",
  "description": "Birth certificate Anoncred Credential Definition",
  "version": "1.0.0",
  "tag": "Licence",
  "author": "{{ISSUER_DID_SHORT}}",
  "schemaId": "http://host.docker.internal:8080/cloud-agent/schema-registry/schemas/{{SCHEMA_ID}}",
  "signatureType": "CL",
  "supportRevocation": true
}
```

### 2. Create the Credential Definition Record

1. Use your preferred REST API client, such as Postman or Insomnia, or utilize a client stub that's generated based on the OpenAPI specification.

2. In your API client, initiate a new POST request to the `/credential-definition-registry/definitions/` endpoint.

Please note: The `author` field value should align with the short form of a PRISM DID previously created by the same agent. It's okay if this DID is unpublished. You can refer to the [Create DID](../dids/create.md) documentation for more comprehensive details on crafting a PRISM DID.

3. Construct the request body using the following JSON object:

```json
{
  "name": "Birth Certificate location",
  "description": "Birth certificate Anoncred Credential Definition",
  "version": "1.0.0",
  "tag": "Licence",
  "author": "{{ISSUER_DID_SHORT}}",
  "schemaId": "http://host.docker.internal:8080/cloud-agent/schema-registry/schemas/{{SCHEMA_ID}}",
  "signatureType": "CL",
  "supportRevocation": true
}
```

4. Transmit the POST Request to Create the New Credential Definition

Once you've crafted your POST request, send it. Upon success, the server should respond with a GUID that uniquely identifies the new credential definition.

For ease of reference, here's a `curl` example:

```shell
curl -X 'POST' \
  'http://localhost:8080/credential-definition-registry/definitions/' \
  -H 'accept: application/json' \
  -H "apikey: $API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Birth Certificate location",
  "description": "Birth certificate Anoncred Credential Definition",
  "version": "1.0.0",
  "tag": "Licence",
  "author": "{{ISSUER_DID_SHORT}}",
  "schemaId": "http://host.docker.internal:8080/cloud-agent/schema-registry/schemas/{{SCHEMA_ID}}",
  "signatureType": "CL",
  "supportRevocation": true
}
```

A potential response could be:

```json
{
  "guid": "3f86a73f-5b78-39c7-af77-0c16123fa9c2",
  "id": "f2bfbf78-8bd6-4cc6-8b39-b3a25e01e8ea",
  "longId": "did:prism:agent/f2bfbf78-8bd6-4cc6-8b39-b3a25e01e8ea?version=1.0.0",
  "name": "Birth Certificate location",
  "version": "1.0.0",
  "description": "Birth certificate Anoncred Credential Definition",
  "tag": "Licence",
  "author": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff",
  "authored": "2023-03-14T14:41:46.713943Z",
  "schemaId": "http://host.docker.internal:8080/cloud-agent/schema-registry/schemas/{{SCHEMA_ID}}",
  "signatureType": "CL",
  "supportRevocation": true,
  "kind": "CredentialDefinition",
  "self": "/credential-definition-registry/definitions/3f86a73f-5b78-39c7-af77-0c16123fa9c2"
}
```

### 3. Retrieve the Created Credential Definition

To obtain details of the newly created credential definition, send a GET request to the `/credential-definition-registry/definitions/{guid}` endpoint. Replace `{guid}` with the unique GUID returned from the previous creation step.

To exemplify this process, use the following `curl` command:

```shell
curl -X 'GET' \
  'http://localhost:8080/credential-definition-registry/definitions/3f86a73f-5b78-39c7-af77-0c16123fa9c2' \
  -H 'accept: application/json' \
  -H "apikey: $API_KEY"
```

You should receive a response containing the JSON object representing the credential definition you've just established:

```json
{
  "guid": "3f86a73f-5b78-39c7-af77-0c16123fa9c2",
  "id": "f2bfbf78-8bd6-4cc6-8b39-b3a25e01e8ea",
  "longId": "did:prism:agent/f2bfbf78-8bd6-4cc6-8b39-b3a25e01e8ea?version=1.0.0",
  "name": "Birth Certificate location",
  "version": "1.0.0",
  "description": "Birth certificate Anoncred Credential Definition",
  "tag": "Licence",
  "author": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff",
  "authored": "2023-03-14T14:41:46.713943Z",
  "schemaId": "http://host.docker.internal:8080/cloud-agent/schema-registry/schemas/{{SCHEMA_ID}}",
  "signatureType": "CL",
  "supportRevocation": true,
  "kind": "CredentialDefinition",
  "self": "/credential-definition-registry/definitions/3f86a73f-5b78-39c7-af77-0c16123fa9c2"
}
```

Remember, in the Cloud Agent, the combination of author, id, and version uniquely identifies each credential definition. Thus, using the same agent DID as the author, you cannot establish another credential definition with identical id and version values.

### 4. Update the Credential Definition

To update or upgrade an existing credential definition, follow the steps outlined below:

1. Begin with the first step and make necessary modifications to the Credential Definition.
2. Update the `version` value to reflect the changes made. This is important to ensure that each version of the credential definition remains distinct.
3. Create a new credential definition entry with the updated version and schema.

Note: When you make changes to an existing credential definition, it's essential to version the new entry accurately. This ensures clarity and avoids potential conflicts or misunderstandings among different versions of the same definition.

