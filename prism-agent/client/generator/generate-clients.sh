#!/bin/bash
set -e

PRISM_AGENT_VERSION=${VERSION_TAG:13}

# install dependencies
yarn

# generate kotlin models
yarn openapi-generator-cli generate \
    -g kotlin \
    -i oas.yml \
    -o ../kotlin \
    --ignore-file-override ../kotlin/.openapi-generator-ignore \
    --additional-properties=packageName=io.iohk.atala.prism,serializationLibrary=gson

# generate typescript models
yarn openapi-generator-cli generate \
    -g typescript \
    -i oas.yml \
    -o ../typescript \
    --ignore-file-override ../typescript/.openapi-generator-ignore

# generate python models
# yarn openapi-generator-cli generate -g python -i oas.yml --skip-validate-spec -o ../python --ignore-file-override ../python/.openapi-generator-ignore
