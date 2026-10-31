#!/bin/sh
set -eu
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi
printf '%s\n' 'Gradle 8.13 is required. Install Gradle or open this project in Android Studio.' >&2
exit 1
