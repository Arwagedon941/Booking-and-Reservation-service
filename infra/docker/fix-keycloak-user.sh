#!/bin/sh
# Скрипт для исправления пользователя в Keycloak

# Ждем запуска Keycloak
sleep 10

# Настраиваем kcadm
/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin

# Удаляем существующего пользователя (если есть)
/opt/keycloak/bin/kcadm.sh delete users/$(/opt/keycloak/bin/kcadm.sh get users -r app --username user -q | grep -o '"id":"[^"]*' | cut -d'"' -f4) -r app 2>/dev/null || true

# Создаем нового пользователя
/opt/keycloak/bin/kcadm.sh create users -r app -s username=user -s email=user@example.com -s emailVerified=true -s enabled=true

# Получаем ID пользователя
USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r app --username user -q | grep -o '"id":"[^"]*' | cut -d'"' -f4)

# Устанавливаем пароль
/opt/keycloak/bin/kcadm.sh set-password -r app --username user --new-password password

# Убираем required actions
/opt/keycloak/bin/kcadm.sh update users/$USER_ID -r app -s 'requiredActions=[]'

echo "User 'user' created successfully with password 'password'"




