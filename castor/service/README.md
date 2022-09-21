# Castor BB service

## Quickstart

__Running Castor service locally for development__

```bash
docker-compose -f docker/docker-compose-local.yaml up -d
sbt api-server/run
```

---

## Known limitations

### OpenAPI codegen

The usage of `oneOf` / `anyOf` in OpenAPI specification doesn't map nicely to scala code using default template.
This has to be eventually addressed by customizing `mustache` template.

### Castor `worker` process embedded inside `api-server`

Castor worker process which is responsible for async comminucation with other components is temporarily embedded inside `api-server`.
This impose some restrictions on scalability as it requires some coordination of between `worker` instances.
It should be addressed going forward.
