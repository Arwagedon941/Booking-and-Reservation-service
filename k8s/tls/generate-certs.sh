#!/bin/bash
# Генерация self-signed сертификатов для разработки
# Для production используйте cert-manager с Let's Encrypt

DOMAIN="app.local"
DAYS=365

# Создаем директорию для сертификатов
mkdir -p certs

# Генерируем приватный ключ
openssl genrsa -out certs/tls.key 2048

# Генерируем CSR (Certificate Signing Request)
openssl req -new -key certs/tls.key -out certs/tls.csr -subj "/CN=${DOMAIN}/O=Platform/C=KZ"

# Генерируем самоподписанный сертификат
openssl x509 -req -days ${DAYS} -in certs/tls.csr -signkey certs/tls.key -out certs/tls.crt

# Создаем сертификат для Keycloak
openssl genrsa -out certs/keycloak-tls.key 2048
openssl req -new -key certs/keycloak-tls.key -out certs/keycloak-tls.csr -subj "/CN=keycloak.${DOMAIN}/O=Platform/C=KZ"
openssl x509 -req -days ${DAYS} -in certs/keycloak-tls.csr -signkey certs/keycloak-tls.key -out certs/keycloak-tls.crt

echo "Сертификаты созданы в директории certs/"
echo "Для создания Kubernetes Secret выполните:"
echo "kubectl create secret tls api-gateway-tls --cert=certs/tls.crt --key=certs/tls.key -n platform"
echo "kubectl create secret tls keycloak-tls --cert=certs/keycloak-tls.crt --key=certs/keycloak-tls.key -n platform"

