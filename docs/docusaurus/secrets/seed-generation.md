# Creating a PRISM agent wallet seed

## Introduction

PRISM agent utilizes a hierarchical-deterministic key derivation algorithm for managing PRISM DIDs,
which follows the BIP32 standard. In order to generate the required keys,
BIP32 uses a master binary seed serving as the root of the derivation tree,
and all other keys are derived from this seed.
Given that the PRISM agent employs BIP32, it expects a 64-byte binary seed as input.
Various methods exist for generating a byte sequence, each serving different purposes.

PRISM agent does not have any opinion on how the seed should be generated as long as a valid hex string is provided.
However, it is strongly recommended to use high entropy for generating the master seed.
PRISM agent allows customizing the default wallet seed by using the environment variable `DEFAULT_WALLET_SEED`.
Other wallet seeds can also be configured when creating a wallet using REST API.
The variable must contain a 64-byte value encoded in hexadecimal format.

### 1. Static seed

PRISM agent expects any valid 64-byte input for a wallet seed.
Any static 128-character hexadecimal string can be used to simplify the testing.

For example

```sh
# Any of these are valid
DEFAULT_WALLET_SEED=00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
DEFAULT_WALLET_SEED=11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
DEFAULT_WALLET_SEED=0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a
```
__Note: Do not use method this in production.__

### 2. Simple dynamic seed

Users have the option to create a random hexadecimal string of a desired length using scripting languages
like Bash or Python. An example of a Bash script is shown below.

```bash
DEFAULT_WALLET_SEED=$(tr -dc a-f0-9 </dev/urandom | head -c 128 ; echo '')
```
This approach is suitable for basic testing scenarios requiring dynamically generated seeds.
However, for production use, it is advisable to employ a reputable implementation of BIP39
with a high level of entropy. (Refer to the details below for further information.)


### 3. Use BIP39 implementation to generate a seed (recommended)

The [BIP39](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#user-content-From_mnemonic_to_seed)
protocol can be utilized to generate a BIP32 master seed, offering a human-friendly approach to seed management.
Instead of noting down a seemingly random hexadecimal string,
users can write down their mnemonic phrase, making it more convenient to keep track of them.

By using BIP39, users have options to choose a mnemonic phrase length as well as a passphrase.
There are many tools for generating a BIP39 seed including but not limited to:
- <https://iancoleman.io/bip39/> (use the BIP39 seed field which provides a 128-chars hex string)
- [BIP39 - implementations section](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#other-implementations) also provides a list of implementations
