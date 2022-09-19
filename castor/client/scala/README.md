# Castor BB client

## Quickstart

__Generate client from api specifications__

```bash
sbt clean compile
```

## Known issues

### Imperfect sttp code generation

Sttp OpenAPI generator sometime fail to generate the code on some edge case (e.g. schema of a primitive type). During specification development, we should validate OpenAPI spec to make sure that it is compiled and the client is successfully generated.
