# Cloud-agent client generator

This project goal is to generate the models based on the OpenAPI Specification.

## Generating models

Then run the generator scripts:

```bash
cd generator
yarn generate
```

To publish the clients:

```bash
cd generator
yarn publish:clients
```

## Cloud-agent lifecycle

`identus-client-generator` creates the clients after the `cloud-agent-v*` tag is created.

## Supported clients

1. Kotlin
2. Typescript

### Work in progress

1. Python

# Caution note

Some of the OAS3 schema types are not fully supported.

The generated files that are not supported were fixed manually and ignored from the generation, in the `.openapi-generator-ignore` file, therefore it requires a diligence work to keep them updated.
