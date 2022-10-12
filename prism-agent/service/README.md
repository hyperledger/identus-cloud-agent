# PrismAgent service

## Quickstart

### Running PrismAgent service local development

__Environmental setup__

First, we need to publish a set of libraries that is used by `prism-agent`.
Set the environment variable `ATALA_GITHUB_TOKEN` so we can pull `prism-crypto` from Github packages.

__Spin up PrismAgent dependencies__

Then we need to spin up the database for Castor, Pollux and optionally Iris.
The easiest way is to reuse the `./infrastructure/local/docker-compose.yml`
which contains databases and all the services.
It should also apply migration on startup.
Some services may be commented out if not needed during local development.

```bash
# From the root directory
./infrastructure/loca/run.sh
```
Then the services should be availble in the following ports

|service|port|
|---|---|
|castor db|5432|
|pollux db|???|
|iris|8081|

Then configure `prism-agent` to use the services listed above.

---

## Known limitations

### Castor `worker` process embedded inside `prism-agent` runnable

Castor worker process which is responsible for async comminucation with other components is temporarily embedded inside `server`.
This impose some restrictions on scalability as it requires some coordination of between `worker` instances.
