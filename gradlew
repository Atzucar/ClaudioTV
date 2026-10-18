#!/usr/bin/env sh

# Intentar encontrar Java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Ejecutar Gradle usando el tar de la máquina
exec "$JAVACMD" "-Dorg.gradle.appname=$0" -classpath "app/build.gradle.kts" org.gradle.launcher.GradleMain "$@"
