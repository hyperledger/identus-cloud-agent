# Repositories & Relationships

The building blocks are the separation of the projects by capabilities that work together.
That makes the Atala PRISM framework.

This is the main repository for the Atala PRISM V2.

However the BB are separated into several repositories.
And there are dependencies with the Atala PRISM V1.

So here we have a simplified view of all Atala repositories and how are they related together:

```mermaid
flowchart
  subgraph doc
    atala-prism-docs
    atala-prism-interactive-demo-web
  end

  subgraph PRISM_1_4
    atala-prism-spec -->|spec for| atala-prism
    atala-mirror 
    atala-prism-credentials-verification-portal -->|uses node| atala-prism
    atala-qa-automation
    atala-prism
  
    subgraph SDK_PRISM_1_4
      atala-prism-sdk
      atala-prism-sdk-scalajs
    end

    subgraph archive
      atala-legacy
      subgraph to_be_archived
        direction RL
        atala-prism-android-app
        atala-prism-sdk-ios
        atala-prism-sdk-ios-sp
        atala-prism-ios-app
        atala-prism-management-console-web %% -->|backend| 
        atala-prism-browser-extension-wallet
      end
    end

    subgraph empty
      atala-style_-guide[atala-style-guide]
      atala-prism-connect
      atala-prism-vault-sdk
      atala-cocoapods-specs
    end
    atala-prism-connect -->|wannabe| atala-prism
    atala-prism-vault-sdk -->|wannabe| atala-prism
  end

  subgraph coding-challanges-for-candidates
    atala-swetest
  end

  subgraph PRISM_2
    
    atala-prism-building-blocks ---> |currently uses node| atala-prism
    atala-prism-building-blocks --> |currently uses crypto| SDK_PRISM_1_4
    atala-prism-apollo
    atala-prism-didcomm-kmm
    atala-prism-manage 
    atala-prism-products
    atala-web-app-template
    atala-prism-esdk -->|SDK for| atala-prism-building-blocks
    atala-prism-crypto-sdk-sp -->|1.4 Crypto SDK to be replaced by apollo| atala-prism-wallet-sdk-swift
    atala-prism-didcomm-swift -->|Didcomm rust to be replaced by our didcomm-kmm| atala-prism-wallet-sdk-swift
    atala-prism-didcomm-kmm --> atala-prism-wallet-sdk-kmm
    atala-prism-apollo --> atala-prism-wallet-sdk-kmm
  end

  %% infra 
  atala-infra --wannabe-->  atala-prism-infra
  atala-prism-infra --->|infra for| PRISM_1_4
  atala-prism-dev-deployments -->|helm charts and environments managed by| atala-prism-v2-infra
  atala-prism-terraform-modules -->|modules used in| atala-prism-v2-infra 
  atala-prism-v2-infra ---->|infrastructure as code definition for aws| PRISM_2
  atala-prism-v2-infra --> doc




%% click atala-prism-wallet-sdk-kmm href "https://github.com/input-output-hk/atala-prism-wallet-sdk-kmm"
%% click atala-prism href "https://github.com/input-output-hk/atala-prism"
%% click atala-prism-docs href "https://github.com/input-output-hk/atala-prism-docs"
%% click atala-prism-v2-infra href "https://github.com/input-output-hk/atala-prism-v2-infra"
%% click atala-prism-building-blocks href "https://github.com/input-output-hk/atala-prism-building-blocks"
%% click atala-prism-products href "https://github.com/input-output-hk/atala-prism-products"
%% click atala-prism-credentials-verification-portal href "https://github.com/input-output-hk/atala-prism-credentials-verification-portal"
%% click atala-prism-terraform-modules href "https://github.com/input-output-hk/atala-prism-terraform-modules"
%% click atala-prism-interactive-demo-web href "https://github.com/input-output-hk/atala-prism-interactive-demo-web"
%% click atala-prism-dev-deployments href "https://github.com/input-output-hk/atala-prism-dev-deployments"
%% click atala-prism-esdk href "https://github.com/input-output-hk/atala-prism-esdk"
%% click atala-prism-sdk href "https://github.com/input-output-hk/atala-prism-sdk"
%% click atala-qa-automation href "https://github.com/input-output-hk/atala-qa-automation"
%% click atala-prism-apollo href "https://github.com/input-output-hk/atala-prism-apollo"
%% click atala-prism-didcomm-kmm href "https://github.com/input-output-hk/atala-prism-didcomm-kmm"
%% click atala-prism-crypto-sdk-sp href "https://github.com/input-output-hk/atala-prism-crypto-sdk-sp"
%% click atala-prism-didcomm-swift href "https://github.com/input-output-hk/atala-prism-didcomm-swift"
%% click atala-prism-infra href "https://github.com/input-output-hk/atala-prism-infra"
%% click atala-prism-android-app href "https://github.com/input-output-hk/atala-prism-android-app"
%% click atala-prism-manage href "https://github.com/input-output-hk/atala-prism-manage"
%% click atala-web-app-template href "https://github.com/input-output-hk/atala-web-app-template"
%% click atala-prism-connect href "https://github.com/input-output-hk/atala-prism-connect"
%% click atala-prism-sdk-ios href "https://github.com/input-output-hk/atala-prism-sdk-ios"
%% click atala-prism-management-console-web href "https://github.com/input-output-hk/atala-prism-management-console-web"
%% click atala-prism-sdk-ios-sp href "https://github.com/input-output-hk/atala-prism-sdk-ios-sp"
%% click atala-mirror href "https://github.com/input-output-hk/atala-mirror"
%% click atala-prism-ios-app href "https://github.com/input-output-hk/atala-prism-ios-app"
%% click atala-prism-browser-extension-wallet href "https://github.com/input-output-hk/atala-prism-browser-extension-wallet"
%% click atala-prism-sdk-scalajs href "https://github.com/input-output-hk/atala-prism-sdk-scalajs"
%% click atala-prism-spec href "https://github.com/input-output-hk/atala-prism-spec"
%% click atala-prism-vault-sdk href "https://github.com/input-output-hk/atala-prism-vault-sdk"
%% click atala-infra href "https://github.com/input-output-hk/atala-infra"
%% click atala-legacy href "https://github.com/input-output-hk/atala-legacy"
%% click atala-swetest href "https://github.com/input-output-hk/atala-swetest"
%% click atala-cocoapods-specs href "https://github.com/input-output-hk/atala-cocoapods-specs"
%% click atala-style_-guide href "https://github.com/input-output-hk/atala-style-guide"
%% 
%% style PRISM_2 fill:#f969
```
