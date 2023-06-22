# [WIP] ANONCREDS

See [anoncreds-rs repo](https://github.com/hyperledger/anoncreds-rs) & [ce-anoncreds-scala repo](https://github.com/input-output-hk/ce-anoncreds-scala).

See [ATL-4524 - [POLUX] Analyse usage in Scala](https://input-output.atlassian.net/browse/ATL-4524?focusedCommentId=173837)
https://github.com/hyperledger/anoncreds-rs/releases/tag/v0.1.0-dev.15


# Build the NATIVE lib 

Build the NATIVE anoncred lib from IOHK's fork of the anoncreds-rs [https://github.com/input-output-hk/anoncreds-rs](https://github.com/input-output-hk/anoncreds-rs)

Go into the folder `uniffi` and build the Native lib with:

- `cargo build --release --target x86_64-unknown-linux-gnu` (For Linux)
- `cargo build --release --target x86_64-unknown-linux-gnu` (For Mac)
