#!/bin/sh
# Maven Wrapper script
# Downloads Maven if not present, then runs it

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    MAVEN_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2-)
fi

MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.6"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven..."
    mkdir -p "$MAVEN_HOME"
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "$MAVEN_URL" -o "$MAVEN_HOME/maven.zip"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "$MAVEN_URL" -O "$MAVEN_HOME/maven.zip"
    else
        echo "Error: curl or wget required"
        exit 1
    fi
    cd "$MAVEN_HOME"
    unzip -q maven.zip
    rm maven.zip
    cd "$MAVEN_PROJECTBASEDIR"
fi

MAVEN_BIN=$(find "$MAVEN_HOME" -name "mvn" -path "*/bin/mvn" | head -1)
exec "$MAVEN_BIN" "$@"
