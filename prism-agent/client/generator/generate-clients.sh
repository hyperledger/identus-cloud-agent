#!/bin/bash
set -e

# open api cli generator is not compatible with 3.1.0
yq e -i '.openapi = "3.0.3"' ../../service/api/http/prism-agent-openapi-spec.yaml

# generate kotlin models
yarn openapi-generator-cli generate \
    -g kotlin \
    -i ../../service/api/http/prism-agent-openapi-spec.yaml \
    -o ../kotlin \
    --ignore-file-override ../kotlin/.openapi-generator-ignore \
    --additional-properties=packageName=io.iohk.atala.prism,serializationLibrary=gson,enumPropertyNaming=UPPERCASE

# generate typescript models
yarn openapi-generator-cli generate \
    -g typescript \
    -i ../../service/api/http/prism-agent-openapi-spec.yaml \
    -o ../typescript \
    --ignore-file-override ../typescript/.openapi-generator-ignore

# generate python models
# yarn openapi-generator-cli generate -g python -i oas.yml --skip-validate-spec -o ../python --ignore-file-override ../python/.openapi-generator-ignore
