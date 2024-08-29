# How to run issuance flow

## Prerequisites

- Docker installed v2.24.0 or later

### 1. Spin up the agent stack with pre-configured Keycloak

```bash
docker-compose up
```

The Keycloak UI is available at `http://localhost:9980` and the admin username is `admin` with password `admin`.

### 2. Run the issuance demo script

Build the demo application and run it using

```bash
docker build -t identus-oid4vci-demo:latest ./demo
docker run --network <NETWORK_NAME> -it identus-oid4vci-demo:latest
```

where `NETWORK_NAME` is the docker-compose network name from agent stack.
By default, this value should be `st-oid4vci_default`.

- 2.1 Follow the instructions in the terminal. The holder will then be asked to log in via a browser
- 2.2 Enter the username `alice` and password `1234` to log in
- 2.3 Grant access for the scopes displayed on the consent UI

The credential should be issued at the end of the flow and logged to the terminal.
