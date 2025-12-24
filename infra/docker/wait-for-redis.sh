#!/bin/sh
# Скрипт ожидания готовности Redis

set -e

host="$1"
port="$2"
shift 2
cmd="$@"

echo "Ожидание Redis на $host:$port..."

until nc -z "$host" "$port"; do
  >&2 echo "Redis недоступен - ожидание..."
  sleep 1
done

>&2 echo "Redis готов!"
exec $cmd



