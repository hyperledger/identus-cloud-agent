# [WIP] ANONCREDS

See [anoncreds-rs repo](https://github.com/hyperledger/anoncreds-rs) & [ce-anoncreds-scala repo](https://github.com/input-output-hk/ce-anoncreds-scala)

See [ATL-4524 - [POLUX] Analyse usage in Scala](https://input-output.atlassian.net/browse/ATL-4524?focusedCommentId=173837)
https://github.com/hyperledger/anoncreds-rs/releases/tag/v0.1.0-dev.15

## Linux

**build** the lib `.so` with the sbt command `buildShim`

**test** with the sbt command `polluxAnoncreds/test`

## [WIP/TODO] Multi plataform (linux with x86_64 and Mac with arm64)

**Copy the existing cbindgen C header from the anoncreds-rs to the ce-anoncreds-scala project:**
```shell
wget -P src/main/c https://raw.githubusercontent.com/hyperledger/anoncreds-rs/v0.1.0-dev.15/include/libanoncreds.h
```


**Compile the C wrapper/shim found in the ce-anoncreds-scala project:**
```shell
# Create output folder first
mkdir native-lib

# Compile the shim to produce a '.o' object file
# From a AARCH64 platform
gcc -O2 \
        -fno-omit-frame-pointer \
        -fno-strict-aliasing \
        -D_REENTRANT \
        -fno-common \
        -W -Wall -Wno-unused -Wno-parentheses \
        -I"target" \
        -I"src/main/c" \
        -arch x86_64 -arch arm64 \
        -c src/main/c/anoncreds-shim.c \
        -o "native-lib/anoncreds-shim.o"

# From a X86_64 platform
gcc -O2 \
        -fno-omit-frame-pointer \
        -fno-strict-aliasing \
        -D_REENTRANT \
        -fno-common \
        -W -Wall -Wno-unused -Wno-parentheses \
        -I"target" \
        -I"src/main/c" \
        -c src/main/c/anoncreds-shim.c \
        -o "native-lib/anoncreds-shim.o"
```

**Download the native libraries from the anoncreds-rs project for macOS Darwin architectures and extract them in the native-lib folder:**
```shell
# AARCH64 platform
wget -qO- https://github.com/hyperledger/anoncreds-rs/releases/download/v0.1.0-dev.15/library-darwin-aarch64.tar.gz \
    | tar -zxvO ./libanoncreds.dylib > native-lib/libanoncreds-darwin-aarch64.dylib

# X86_64 platform
wget -qO- https://github.com/hyperledger/anoncreds-rs/releases/download/v0.1.0-dev.15/library-darwin-x86_64.tar.gz \
    | tar -zxvO ./libanoncreds.dylib > native-lib/libanoncreds-darwin-x86_64.dylib
```

**Combine the two libraries into a single universal one using lipo:**
```shell
# From a AARCH64 platform
lipo -create \
    native-lib/libanoncreds-darwin-aarch64.dylib \
    native-lib/libanoncreds-darwin-x86_64.dylib \
    -output native-lib/libanoncreds.dylib

# From a X86_64 platform
llvm-lipo -create \
    native-lib/libanoncreds-darwin-aarch64.dylib \
    native-lib/libanoncreds-darwin-x86_64.dylib \
    -output native-lib/libanoncreds.dylib
```

**Link the anoncreds-shim object file for both platforms and combine them into a single universal library:**
```shell
gcc -v -o native-lib/libanoncreds-shim-aarch64.dylib \
    # -arch arm64 
    -dynamiclib native-lib/anoncreds-shim.o \
    -lm -Lnative-lib -llibanoncreds-darwin-aarch64
    
gcc -v -o native-lib/libanoncreds-shim-x86_64.dylib \
    # -arch x86_64 \
    -dynamiclib native-lib/anoncreds-shim.o -lm \
    -Lnative-lib -lanoncreds-darwin-x86_64

# From a AARCH64 platform
lipo -create \
    native-lib/libanoncreds-shim-aarch64.dylib \
    native-lib/libanoncreds-shim-x86_64.dylib \
    -output native-lib/libanoncreds-shim.dylib

# From a X86_64 platform
llvm-lipo -create \
    native-lib/libanoncreds-shim-aarch64.dylib \
    native-lib/libanoncreds-shim-x86_64.dylib \
    -output native-lib/libanoncreds-shim.dylib
```
## Check JAR

The jar must contens the native jars
`jar tf ./pollux/lib/anoncreds/target/scala-3.2.2/pollux-anoncreds_3-1.3.0-SNAPSHOT.jar`