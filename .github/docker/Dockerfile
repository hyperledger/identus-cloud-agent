FROM openjdk:22-jdk-slim

RUN apt-get update && \
    apt-get install --no-install-recommends --yes \
    git=1:2.* \
    curl=7.* \
    wget=1.21.* \
    unzip=6.* \
    zip=3.* \
    jq=1.* \
    python3=3.11.* \
    python3-pip=23.* \
    python3-setuptools=66.* \
    python3-wheel=0.38.* \
    gnupg=2.2.* \
    default-jre=2:1.* \
    build-essential=12.* && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --no-default-keyring --keyring gnupg-ring:/etc/apt/trusted.gpg.d/scalasbt-release.gpg --import && \
    chmod 644 /etc/apt/trusted.gpg.d/scalasbt-release.gpg && \
    apt-get --allow-releaseinfo-change update && \
    apt-get install --no-install-recommends --yes \
    sbt=1.4.* \
    && rm -rf /var/lib/apt/lists/*

CMD [ "/bin/bash" ]
