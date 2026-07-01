#!/bin/bash

JVM_ARGS="-XX:+UseZGC -XX:+ZGenerational -XX:+DisableExplicitGC --add-opens java.base/sun.misc=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"

if [ -n "$IN_DOCKER" ]; then
    java $JVM_ARGS -jar /app/server.jar
else
    java $JVM_ARGS -jar ./server.jar
fi