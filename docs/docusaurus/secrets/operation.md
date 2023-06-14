# Operating a PRISM agent with secret storage

## Introduction

PRISM agent is a cloud-based agent within the SSI ecosystem that facilitates user
DID (Decentralized Identifier) management. To generate a DID key material,
the software relies on a seed, following the BIP32 / BIP39 standards.
The system operators have the option to either provide their own seed or
allow the software to generate one automatically. However, in a production environment,
it is crucial for the system operators to explicitly supply the seed to the agent.
This ensures full control over the DID key material and guarantees secure management of user identities.

The PRISM agent has a default configuration of starting in development mode.
This behavior can be modified using the `DEV_MODE` environment variable,
which accepts the value `true` or `false` to indicate the desired mode.
In development mode, the agent can start with or without a user-specified seed.
To provide a specific seed, set the `WALLET_SEED` environment variable with a
BIP32 binary master seed represented as a hexadecimal string.
If `WALLET_SEED` is not provided, the agent will generate one automatically.
However, if `DEV_MODE=false` and the `WALLET_SEED` is not supplied,
the agent will fail to initialize. This requirement is crucial to maintain secure
and controlled management of the system, given the agent's lack of seed persistence.

__Note that it is important to set `DEV_MODE=false` for the production instance.__

PRISM agent uses the following environment variables for secret management.

| Name          | Description                                          | Default                 |
|---------------|------------------------------------------------------|-------------------------|
| `DEV_MODE`    | Whether PRISM agent should start in development mode | `true`                  |
| `VAULT_TOKEN` | The token for accessing HashiCorp Vault              | `root`                  |
| `VAULT_ADDR`  | The address which PRISM agent can reach the Vault    | `http://localhost:8200` |
| `WALLET_SEED` | The seed used for DID key management                 | -                       |

## Automatic seed generation in the development mode

When starting the agent in development mode without setting the `WALLET_SEED`,
it automatically generates a new seed, which is logged in the console for convenience.
However, this mode is insecure and intended solely for development purposes.
It's important to note that the agent does not persist the seed, so it will be regenerated
upon each restart, potentially resulting in unintended behavior.

Leaving the `WALLET_SEED` undefined, the agent should automatically generate a seed on startup.
When starting the PRISM agent locally the following log should appear in the console.

```
level=INFO message="Resolving a wallet seed using WALLET_SEED environemnt variable"
level=INFO message="WALLET_SEED environment is not found."
level=INFO message="Generating a new wallet seed"
INFO  org.bitcoinj.crypto.MnemonicCode - PBKDF2 took 51.59 ms
level=INFO message="New seed generated : 1044f87e445ca10c537688a9645d738bb48747aee39396e4769257268a7d996575a7913028dc30c455b9faec264a66fb7d4f1f46ddda20a9acc04f77113e43cb ([lobster, nice, congress, chair, offer, security, club, live, wide, drum, fringe, sea, situate, sugar, pear, canoe, caught, embody, health, tell, mimic, spray, kid, parent])"
```
The log indicates an attempt to read the seed from the environment variable initially.
As the variable is not set, the log message includes a binary seed (encode as hex string)
and a 24-word mnemonic phrase (BIP39 compliant).

## Specify a wallet seed in the development mode

Similar to the previous example, set `WALLET_SEED=<seed_hex>` to specify the `WALLET_SEED` value.
When starting the agent, the log message will confirm the retrieval of the seed from
the `WALLET_SEED` variable.

```
level=INFO message="Resolving a wallet seed using WALLET_SEED environemnt variable"
```

## Running in production

For production use of the PRISM agent, ensure the `WALLET_SEED` is set.
If the `WALLET_SEED` is not provided and `DEV_MODE=false`, the agent will fail to
start and display the following error message.

```
level=ERROR java.lang.Exception: WALLET_SEED must be present when running with DEV_MODE=false
```
