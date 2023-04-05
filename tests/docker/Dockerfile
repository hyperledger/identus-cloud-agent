FROM openjdk:11

# Define home
ENV HOME=/root
# Proper pipe commands failure checking
SHELL [ "/bin/bash", "-o", "pipefail", "-c" ]
# Install Linux dependencies
RUN wget --progress=dot:giga -O - https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - && \
    echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    wget --progress=dot:giga -O - "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add  && \
    apt-get --allow-releaseinfo-change update && \
    apt-get install --no-install-recommends --yes \
    build-essential=12.* \
    python3=3.9.* \
    python3-dev=3.9.* \
    python3-pip=20.* \
    python3-setuptools=52.* \
    python3-venv=3.9.* \
    python3-testresources=2.* \
    apt-transport-https=2.2.* \
    lsb-release=11.* \
    postgresql=13* \
    libxml2-utils=2.9.* \
    yarn=1.22.* \
    sbt=1.4.* \
    jq=1.* \
    xxd=2:* \
    zip=3.* \
    && rm -rf /var/lib/apt/lists/* && \
    ln -sf /usr/bin/python3 /usr/bin/python

WORKDIR ${HOME}
CMD [ "/bin/bash" ]
