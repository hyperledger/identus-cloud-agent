#!/usr/bin/env bash

RELEASES=https://github.com/input-output-hk/anoncreds-rs/releases/download/
VERSION=expose_presentation_methods_in_uniffi
SHA="16a7178ca979f7643788f6ebf8189131ab922a53"

mkdir NATIVE_new
mkdir NATIVE_new/darwin-aarch64
mkdir NATIVE_new/darwin-x86-64
mkdir NATIVE_new/linux-aarch64
mkdir NATIVE_new/linux-x86-64

wget -c $RELEASES/$VERSION/library-darwin-aarch64-$SHA.tar.gz -O - | tar -xz -C NATIVE_new/darwin-aarch64
wget -c $RELEASES/$VERSION/library-darwin-x86_64-$SHA.tar.gz -O - | tar -xz -C NATIVE_new/darwin-x86-64
wget -c $RELEASES/$VERSION/library-linux-aarch64-$SHA.tar.gz -O - | tar -xz -C NATIVE_new/linux-aarch64
wget -c $RELEASES/$VERSION/library-linux-x86_64-$SHA.tar.gz -O - | tar -xz -C NATIVE_new/linux-x86-64

rename  'libanoncreds_uniffi.so' 'libuniffi_anoncreds.so' NATIVE_new/**/*.so
rename  'libanoncreds_uniffi.dylib' 'libuniffi_anoncreds.dylib' NATIVE_new/**/*.dylib


## TODO missing anoncreds-jvm-1.0-SNAPSHOT.jar
## run https://github.com/input-output-hk/anoncreds-rs/blob/main/uniffi/build-release-linux.sh
## copy jar from uniffi/output-frameworks/anoncreds-jvm/build/libs/anoncreds-jvm-1.0-SNAPSHOT.jar