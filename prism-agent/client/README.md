# Usage

Every command should be executed inside the `generator` folder.

## Commands

### Generate models

```./generate.sh```

### Clean generated models

```./clean_output.sh```

## Prism-agent lifecycle

`prism-client-generator` creates the clients after the semantic-release process is done.

## Supported clients

1. Kotlin
2. Typescript

### Work in progress

1. Python

# Caution note

Some of the OAS3 schema types are not fully supported.

The generated files that are not supported were fixed manually and ignored from the creation therefore it requires a diligence work to keep them updated.
