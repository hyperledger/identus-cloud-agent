# Update the credential schema

The PRISM platform v2.0 exposes REST API for creation, fetching, and searching the credential schema records.

The OpenAPI specification and ReDoc documentation describe the endpoint.

In this document, you can find step-by-step instructions for updating the credential schema.

After creation, updating the credential schema record is not possible.
If you need to create a similar schema but with additional fields or a different description, or metadata, you need to create the credential schema record with the same `id` and a higher `version`.

## Step-by-step guide

The following guide demonstrates how to update a driving license credential schema.

### 1. Define the updated JSON Schema for the Verifiable Credential

Assume that you need to update the credential schema from the previous tutorial.
So, there is an existing driving license, and the [verifiable credential](/docs/concepts/glossary#verifiable-credential) must additionally include two fields:

- bloodType - the blood type of the driver
- organDonor - indicates whether or not the person is an organ donor

The blood type on a driver's license is represented using the ABO blood group system, and
potentially represented as A+, A-, B+, B-, AB+, AB-, O+, or O-.
So, assume that this set of values must be enforced by the schema definition using the following regex:

```regexp
^(A|B|AB|O)[+-]?$
```

At the same time, the organ donor must represent a binary value: `true`/`false`, `yes`/`no`, depending on the
jurisdiction, and it also might be `unknown` and must be enforced by the schema definition using the `enum` keyword:

```yaml
  enum:
    - true
    - false
    - yes
    - no
    - unknown
```

> **Note**: As the original credential schema allows `additionalProperties` to be defined, we assume that two additional claims must get added to the `required` attributes.

As the change to the credential schema is backward compatible, the next version can be `1.1.0`

---

The JSON Schema changes must be defined as follows:

```json
{
   "$id": "https://example.com/driving-license-1.1.0",
   "$schema": "https://json-schema.org/draft/2020-12/schema",
   "description": "Driving License",
   "type": "object",
   "properties": {
      "emailAddress": {
         "type": "string",
         "format": "email"
      },
      "givenName": {
         "type": "string"
      },
      "familyName": {
         "type": "string"
      },
      "dateOfIssuance": {
         "type": "string",
         "format": "date-time"
      },
      "drivingLicenseID": {
         "type": "string"
      },
      "drivingClass": {
         "type": "integer"
      },
      "bloodType": {
         "type": "string",
         "pattern": "^(A|B|AB|O)[+-]?$"
      },
      "donorStatus": {
         "type": "string",
         "enum": [
            "true",
            "false",
            "yes",
            "no"
         ]
      }
   },
   "required": [
      "emailAddress",
      "familyName",
      "dateOfIssuance",
      "drivingLicenseID",
      "drivingClass",
      "bloodType",
      "donorStatus"
   ],
   "additionalProperties": true
}
```

### 2. Update the credential schema record

1. Open your preferred REST API client, such as Postman or Insomnia, or use the client stub generated based on the
   OpenAPI specification.

2. In the client, create a new PUT request to the `/prism-agent/schema-registry/schemas/{id}` endpoint, where `id` is a
   locally unique credential schema id, formatted as a URL.

Note that the value of the `author` field must match the short form of a PRISM DID that has been created using the same agent. An unpublished DID is sufficient. Please refer to the [Create DID](../dids/create.md) documentation page for more details on how to create a PRISM DID. 

In the request body, create a JSON object:
   
```json
{
  "name": "driving-license",
  "version": "1.1.0",
  "description": "Driving License Schema",
  "type": "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json",
  "author": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff",
  "schema": {
     "$id": "https://example.com/driving-license-1.1.0",
     "$schema": "https://json-schema.org/draft/2020-12/schema",
     "description": "Driving License",
     "type": "object",
     "properties": {
        "emailAddress": {
           "type": "string",
           "format": "email"
        },
        "givenName": {
           "type": "string"
        },
        "familyName": {
           "type": "string"
        },
        "dateOfIssuance": {
           "type": "string",
           "format": "date-time"
        },
        "drivingLicenseID": {
           "type": "string"
        },
        "drivingClass": {
           "type": "integer"
        },
        "bloodType": {
           "type": "string",
           "pattern": "^(A|B|AB|O)[+-]?$"
        },
        "donorStatus": {
           "type": "string",
           "enum": [
              "true",
              "false",
              "yes",
              "no"
           ]
        }
     },
     "required": [
        "emailAddress",
        "familyName",
        "dateOfIssuance",
        "drivingLicenseID",
        "drivingClass",
        "bloodType",
        "donorStatus"
     ],
     "additionalProperties": true
  },
  "tags": [
    "driving",
    "license"
  ]
}
```

The curl example might be the following:

```shell
curl -X 'PUT' \
  'http://localhost:8080/prism-agent/schema-registry/schemas/f2bfbf78-8bd6-4cc6-8b39-b3a25e01e8ea' \
  -H 'accept: application/json' \
  -H "apikey: $API_KEY" \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "driving-license",
  "version": "1.1.0",
  "description": "Driving License Schema",
  "type": "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json",
  "author": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff",
  "schema": {
     "$id": "https://example.com/driving-license-1.1.0",
     "$schema": "https://json-schema.org/draft/2020-12/schema",
     "description": "Driving License",
     "type": "object",
     "properties": {
        "emailAddress": {
           "type": "string",
           "format": "email"
        },
        "givenName": {
           "type": "string"
        },
        "familyName": {
           "type": "string"
        },
        "dateOfIssuance": {
           "type": "string",
           "format": "date-time"
        },
        "drivingLicenseID": {
           "type": "string"
        },
        "drivingClass": {
           "type": "integer"
        },
        "bloodType": {
           "type": "string",
           "pattern": "^(A|B|AB|O)[+-]?$"
        },
        "donorStatus": {
           "type": "string",
           "enum": [
              "true",
              "false",
              "yes",
              "no"
           ]
        }
     },
     "required": [
        "emailAddress",
        "familyName",
        "dateOfIssuance",
        "drivingLicenseID",
        "drivingClass",
        "bloodType",
        "donorStatus"
     ],
     "additionalProperties": true
  },
  "tags": [
    "driving",
    "license"
  ]
}'
```

...and response might be the following:

```json
{
   "guid": "3f86a73f-5b78-39c7-af77-0c16123fa9c2",
   "id": "f2bfbf78-8bd6-4cc6-8b39-b3a25e01e8ea",
   "longId": "did:prism:agent/f2bfbf78-8bd6-4cc6-8b39-b3a25e01e8ea?version=1.1.0",
   "name": "driving-license",
   "version": "1.1.0",
   "description": "Driving License Schema",
   "type": "https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json",
   "author": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff",
   "authored": "2023-03-14T14:41:46.713943Z",
   "tags": [
      "driving",
      "license"
   ],
   "schema": {
      "$id": "https://example.com/driving-license-1.1.0",
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "description": "Driving License",
      "type": "object",
      "properties": {
         "emailAddress": {
            "type": "string",
            "format": "email"
         },
         "givenName": {
            "type": "string"
         },
         "familyName": {
            "type": "string"
         },
         "dateOfIssuance": {
            "type": "string",
            "format": "date-time"
         },
         "drivingLicenseID": {
            "type": "string"
         },
         "drivingClass": {
            "type": "integer"
         },
         "bloodType": {
            "type": "string",
            "pattern": "^(A|B|AB|O)[+-]?$"
         },
         "donorStatus": {
            "type": "string",
            "enum": [
               "true",
               "false",
               "yes",
               "no"
            ]
         }
      },
      "required": [
         "emailAddress",
         "familyName",
         "dateOfIssuance",
         "drivingLicenseID",
         "drivingClass",
         "bloodType",
         "donorStatus"
      ],
      "additionalProperties": true
   },
   "kind": "CredentialSchema",
   "self": "/schema-registry/schemas/3f86a73f-5b78-39c7-af77-0c16123fa9c2"
}
```