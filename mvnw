#!/bin/sh
# AuthEase Maven Wrapper Shell Script

if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

if [ -x "$HOME/tools/maven/bin/mvn" ]; then
    exec "$HOME/tools/maven/bin/mvn" "$@"
fi

echo "Maven not found. Please install Maven or add to PATH." >&2
exit 1
