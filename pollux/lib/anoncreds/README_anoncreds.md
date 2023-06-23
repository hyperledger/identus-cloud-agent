# ANONCREDS

The IOHK's anoncreds-rs repository is on [https://github.com/input-output-hk/anoncreds-rs](https://github.com/input-output-hk/anoncreds-rs)

See [anoncreds-rs repo](https://github.com/hyperledger/anoncreds-rs) & [ce-anoncreds-scala repo](https://github.com/input-output-hk/ce-anoncreds-scala).

See [ATL-4524 - [POLUX] Analyse usage in Scala](https://input-output.atlassian.net/browse/ATL-4524?focusedCommentId=173837)
https://github.com/hyperledger/anoncreds-rs/releases/tag/v0.1.0-dev.15

## ANONCREDS uniffi lib description

The file `anoncreds.udl` describes the warp interface generated with Uniffi.
The file can be found in the IOHK's anoncreds-rs repository [https://github.com/input-output-hk/anoncreds-rs/blob/main/uniffi/src/anoncreds.udl](https://github.com/input-output-hk/anoncreds-rs/blob/main/uniffi/src/anoncreds.udl)

## Build the NATIVE lib 

Build the NATIVE anoncred lib from IOHK's fork of the anoncreds-rs [https://github.com/input-output-hk/anoncreds-rs](https://github.com/input-output-hk/anoncreds-rs)

Go into the folder `uniffi` and build the Native lib with:

- `cargo build --release --target x86_64-unknown-linux-gnu` (For Linux with x86_64)
- `cargo build --release --target x86_64-apple-darwin` (For Mac with x86_64)
- `cargo build --release --target aarch64-apple-darwin` (For Mac with arm)
