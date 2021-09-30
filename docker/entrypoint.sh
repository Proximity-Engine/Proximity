#!/bin/sh
set -e

# if first arg is an option, run proximity with those options
if [ "${1#-}" != "$1" ]; then
  java -jar proximity.jar "$@"
else
  exec "$@"
fi

