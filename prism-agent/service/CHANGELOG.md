# [prism-agent-v0.7.0](https://github.com/input-output-hk/atala-prism-building-blocks/compare/prism-agent-v0.6.0...prism-agent-v0.7.0) (2022-12-01)


### Features

* **pollux:** alight the OAS for schema registry ([#189](https://github.com/input-output-hk/atala-prism-building-blocks/issues/189)) ([4528c57](https://github.com/input-output-hk/atala-prism-building-blocks/commit/4528c573fc89b9af60f6bc014e7f34433a181cb8))
* **pollux:** cleanup the code of IssueCredentialApi ([16d5fdb](https://github.com/input-output-hk/atala-prism-building-blocks/commit/16d5fdbadf20c1597bf42b4e366f71623804dfc4))
* **pollux:** cleanup the OAS from Issue Credentials and other unused tags ([79170f8](https://github.com/input-output-hk/atala-prism-building-blocks/commit/79170f8722053de9e477118f3f9443c97f27c512))
* **prism-agent:** upgrade castor on prism-agent ([#141](https://github.com/input-output-hk/atala-prism-building-blocks/issues/141)) ([e85e7c0](https://github.com/input-output-hk/atala-prism-building-blocks/commit/e85e7c09019f3e1d55f36b73fa081f42c19d4218))

# [prism-agent-v0.6.0](https://github.com/input-output-hk/atala-prism-building-blocks/compare/prism-agent-v0.5.0...prism-agent-v0.6.0) (2022-11-29)


### Features

* **prism-agent:** add connect/issue doc + allow local execution of multiple Prism Agent instances ([#178](https://github.com/input-output-hk/atala-prism-building-blocks/issues/178)) ([dc8d86b](https://github.com/input-output-hk/atala-prism-building-blocks/commit/dc8d86b1ba87d747c5ac0089573ddd8c2ab62f5e))

# [prism-agent-v0.5.0](https://github.com/input-output-hk/atala-prism-building-blocks/compare/prism-agent-v0.4.0...prism-agent-v0.5.0) (2022-11-28)


### Features

* **pollux:** schema registry lookup and verification policies REST API ATL-1334 ([#168](https://github.com/input-output-hk/atala-prism-building-blocks/issues/168)) ([d75b36b](https://github.com/input-output-hk/atala-prism-building-blocks/commit/d75b36bd472c1cc2f15d775596a2675cda469754))
* **prism-agent:** implement Connect flow ([#130](https://github.com/input-output-hk/atala-prism-building-blocks/issues/130)) ([f7cba3b](https://github.com/input-output-hk/atala-prism-building-blocks/commit/f7cba3b18b1e03cd51db144519412fbbbe1585d0))
* **prism-agent:** make didcomm service url configurable ([#173](https://github.com/input-output-hk/atala-prism-building-blocks/issues/173)) ([b162172](https://github.com/input-output-hk/atala-prism-building-blocks/commit/b16217271e38c30cf680e193121baa7122fff67f))

# [prism-agent-v0.4.0](https://github.com/input-output-hk/atala-prism-building-blocks/compare/prism-agent-v0.3.0...prism-agent-v0.4.0) (2022-11-24)


### Features

* **apollo:** add schema registry to the agent using Tapir library. ATL-1334  ([#94](https://github.com/input-output-hk/atala-prism-building-blocks/issues/94)) ([b3cf828](https://github.com/input-output-hk/atala-prism-building-blocks/commit/b3cf828d001f7499c414e9dc559f5152997445e6))

# [prism-agent-v0.3.0](https://github.com/input-output-hk/atala-prism-building-blocks/compare/prism-agent-v0.2.0...prism-agent-v0.3.0) (2022-11-21)


### Features

* **prism-agent:** implement Issue Credential v2 protocol ([#146](https://github.com/input-output-hk/atala-prism-building-blocks/issues/146)) ([f3cb60e](https://github.com/input-output-hk/atala-prism-building-blocks/commit/f3cb60eb0d4dce73025326d64a012e580f581a7c)), closes [#92](https://github.com/input-output-hk/atala-prism-building-blocks/issues/92)

# [prism-agent-v0.2.0](https://github.com/input-output-hk/atala-prism-building-blocks/compare/prism-agent-v0.1.0...prism-agent-v0.2.0) (2022-11-15)


### Features

* **prism-agent:** implement Flyway migrations from castor and pollux library and call on agent startup ([#117](https://github.com/input-output-hk/atala-prism-building-blocks/issues/117)) ([67bd340](https://github.com/input-output-hk/atala-prism-building-blocks/commit/67bd340e25e4a44118398746268ee25f5cf09477))

# [prism-agent-v0.1.0](https://github.com/input-output-hk/atala-prism-building-blocks/compare/prism-agent-v0.0.1...prism-agent-v0.1.0) (2022-11-11)


### Bug Fixes

* **prism-agent:** reuse db connection for background job ([#102](https://github.com/input-output-hk/atala-prism-building-blocks/issues/102)) ([a873090](https://github.com/input-output-hk/atala-prism-building-blocks/commit/a873090fdd2560eb78263060d502946f142b0574))


### Features

* **agent:** [ATL-2005] implement REST API for credential issuance ([#86](https://github.com/input-output-hk/atala-prism-building-blocks/issues/86)) ([7c1f50a](https://github.com/input-output-hk/atala-prism-building-blocks/commit/7c1f50ab99879beed74c5e0bd03aa51709051527))
* **agent:** integrate key-manage into prism-agent server ([#77](https://github.com/input-output-hk/atala-prism-building-blocks/issues/77)) ([4a88ded](https://github.com/input-output-hk/atala-prism-building-blocks/commit/4a88ded408192d03b744309a4ebaf9f9517a9db2))
* define key-management interface (3) ([#71](https://github.com/input-output-hk/atala-prism-building-blocks/issues/71)) ([47dc3cd](https://github.com/input-output-hk/atala-prism-building-blocks/commit/47dc3cd8857971b96a88ae6f9cf0e2163e6cf08e))
* **prism-agent; mercury; pollux:** Integrate Mercury into prism-agent ATL-2077; ATL-2076 ([#93](https://github.com/input-output-hk/atala-prism-building-blocks/issues/93)) ([db4b21a](https://github.com/input-output-hk/atala-prism-building-blocks/commit/db4b21ac1d6a2c48af502597779acb82f5e03ac0))
* **shared:** Add environmnet configuration for Iris DB and bump scala version in other components to enable build ([#96](https://github.com/input-output-hk/atala-prism-building-blocks/issues/96)) ([a5b583f](https://github.com/input-output-hk/atala-prism-building-blocks/commit/a5b583f445b7efd31987cf9ca017bc544a877986))
