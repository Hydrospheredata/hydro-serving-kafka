#!/bin/bash
# wait-for-manager.sh

set -e

host="$1"
port="$2"
shift
shift
cmd="$@"

until nc -z -v -w5 "$host" "$port"; do
  >&2 echo "sidecar is unavailable - sleeping"
  sleep 3
done

>&2 echo "sidecar is up - executing command"
exec $cmd
