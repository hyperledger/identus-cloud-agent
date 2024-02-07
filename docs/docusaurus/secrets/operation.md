# Operating an agent with secrets

## Introduction

PRISM agent offers a DID (Decentralized Identifier) management solution
which involves creating, storing and using key materials.
To generate a DID key material, the software relies on a seed, following the BIP32 / BIP39 standards.
The system operators have the option to either provide their own seed or
allow the software to generate one automatically. However, in a production environment,
it is crucial for the system operators to explicitly supply the seed to the agent.
This ensures full control over the DID key material and guarantees secure management of user identities.

PRISM agent uses the following environment variables for secret management.

| Name                     | Description                                                     | Default                 |
|--------------------------|-----------------------------------------------------------------|-------------------------|
| `SECRET_STORAGE_BACKEND` | The storage backend that will be used for the secret storage    | `vault`                 |
| `VAULT_ADDR`             | The address which PRISM Agent can reach the Vault               | `http://localhost:8200` |
| `VAULT_TOKEN`            | The token for accessing HashiCorp Vault                         | -                       |
| `VAULT_APPROLE_ROLE_ID`  | The `role_id` for HashiCorp Vault authentication with AppRole   | -                       |
| `VAULT_APPROLE_SECRET_ID`| The `secret_id` for HashiCorp Vault authentication with AppRole | -                       |
| `DEFAULT_WALLET_SEED`    | The seed used for DID key management for the default wallet     | -                       |

## Storage backend configuration

Secret storage supports various backends like the Vault service or Postgres database.
By default, the backend chosen for secret storage is Vault, which is suitable for production environments.
There are multiple supported backend implementations, each catering to specific use cases.

__HashiCorp Vault__

When operating in a production environment, the agent has the option to utilize Vault
as a secure secret storage backend. This choice is deemed suitable for production because
all data is encrypted and it also offers additional security-related capabilities.
By default, the agent uses this backend but the option is configurable.
To utilize this backend, set the `SECRET_STORAGE_BACKEND` variable to `vault`.
The agent expects to read and write secrets to the path `/secret/*`,
to ensure the provisioned permissions.

Example Vault policy

```
path "secret/*" {
    capabilities = ["create", "read", "update", "patch", "delete", "list"]
}
```

HashiCorp Vault provides multiple authentication methods.
One of the simplest methods is [token authentication](https://developer.hashicorp.com/vault/docs/auth/token).
To authenticate using the token, set the environment variable `VAULT_TOKEN`.
The agent prefers token authentication if provided with multiple authentication methods.

Another method is [AppRole authentication](https://developer.hashicorp.com/vault/docs/auth/approle) which is suitable for automatic workflows.
To use AppRole authentication, simply set the environment variable `VAULT_APPROLE_ROLE_ID` and `VAULT_APPROLE_SECRET_ID`.

__Postgres__

Postgres is an alternative backend option for secret storage.
However, this option must be explicitly chosen and will replace Vault.
By opting for Postgres, there is no need for an additional service like Vault,
which simplifies the process of setting up a local development instance.
It utilizes the same database instance as the agent itself. To enable this option,
set the `SECRET_STORAGE_BACKEND` to `postgres`, and it will utilize the same database
configuration as the agent's database. It is important to note that while this option
facilitates an easier development experience, it does not provide a secure method of storing secrets.
The data is not encrypted, making it unsuitable for production use.
