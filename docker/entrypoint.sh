#!/bin/sh
set -e

# if the first arg is an option, pass all args to proximity
# else just exec whatever was supplied
if [ -z "$1" ]; then
  echo "Please either supply options for Proximity, or a command to execute"
elif [ "${1#-}" != "$1" ]; then
  java -jar proximity.jar "$@"
else
  exec "$@"
fi

# if the USER_ID and GROUP_ID env vars are not 0, and the /app/images 
# directory exists, chown /app/images to user USER_ID:GROUP_ID
if [ "$USER_ID" -ne "0" -a "$GROUP_ID" -ne "0" -a -d "/app/images" ]; then
  echo "Running: chown -R $USER_ID:$GROUP_ID /app/images"
  chown -R $USER_ID:$GROUP_ID /app/images
fi
