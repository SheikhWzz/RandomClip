#!/bin/sh
# Gradle wrapper — run "gradle wrapper" once if gradle is installed,
# or open the project in Android Studio (recommended) to sync and build.

DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle Wrapper JAR fehlt."
  echo "Bitte Projekt in Android Studio öffnen und Gradle Sync ausführen,"
  echo "oder lokal ausführen: gradle wrapper"
  exit 1
fi

exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
