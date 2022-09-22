# Castor BB service

## Quickstart

__Running Castor service locally for development__

```bash
docker-compose -f docker/docker-compose-local.yaml up -d
sbt server/run
```

---

## Known limitations

### Castor `worker` process embedded inside `server`

Castor worker process which is responsible for async comminucation with other components is temporarily embedded inside `server`.
This impose some restrictions on scalability as it requires some coordination of between `worker` instances.
It should be addressed going forward.
