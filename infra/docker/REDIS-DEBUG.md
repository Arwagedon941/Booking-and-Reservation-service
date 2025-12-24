# Redis қосылым мәселесін диагностикалау

## Мәселе

Redis контейнері іске қосылады және "Ready to accept connections" хабарламасын көрсетеді, бірақ сервистер оған қосыла алмайды.

## Диагностика

### 1. Redis контейнерінің статусын тексеру

```bash
docker compose ps redis
```

Статус `Up (healthy)` болуы керек. Егер `Up` болса, бірақ `healthy` емес, healthcheck әлі жұмыс істемейді.

### 2. Redis-қа тікелей қосылу

```bash
# Контейнер ішінен
docker compose exec redis redis-cli ping
# Жауап: PONG

# Басқа контейнерден (мысалы, resource-service)
docker compose exec resource-service sh -c "nc -zv redis 6379"
# Жауап: redis (172.x.x.x:6379) open
```

### 3. Сеть тексеру

```bash
# Resource Service контейнерінен Redis-қа ping
docker compose exec resource-service ping -c 3 redis

# Redis IP мекенжайын табу
docker compose exec redis hostname -i

# Resource Service-тен Redis IP-ге қосылу
docker compose exec resource-service ping -c 3 <REDIS_IP>
```

### 4. Порт тексеру

```bash
# Redis контейнерінде портты тексеру
docker compose exec redis netstat -tuln | grep 6379

# Resource Service контейнерінде портты тексеру
docker compose exec resource-service netstat -tuln | grep 6379
```

### 5. Environment айнымалыларын тексеру

```bash
# Resource Service environment
docker compose exec resource-service env | grep REDIS

# Күтілетін нәтиже:
# REDIS_HOST=redis
# REDIS_PORT=6379
```

### 6. Логтарды тексеру

```bash
# Redis логтары (соңғы 50 жол)
docker compose logs redis --tail 50

# Resource Service логтары (Redis қателері)
docker compose logs resource-service | grep -i redis

# Барлық логтар
docker compose logs --tail 100
```

## Жедел шешу

### Вариант 1: Толық қайта іске қосу

```bash
cd infra/docker
docker compose down
docker compose up -d --build
```

### Вариант 2: Redis-ты бөлек іске қосу және күту

```bash
# 1. Redis-ты іске қосу
docker compose up -d redis

# 2. Healthcheck-тің жұмыс істеуін күту (20 секунд)
sleep 20

# 3. Redis-ты тексеру
docker compose exec redis redis-cli ping

# 4. Басқа сервистерді іске қосу
docker compose up -d
```

### Вариант 3: Сеть мәселесін шешу

Егер контейнерлер бір-біріне қосыла алмаса:

```bash
# Барлық контейнерлерді тоқтату
docker compose down

# Docker сетін тазалау
docker network prune -f

# Қайта іске қосу
docker compose up -d
```

## Ең жиі кездесетін мәселелер

### 1. Redis healthcheck әлі жұмыс істемейді

**Белгілері**: `docker compose ps redis` көрсетеді `Up` бірақ `healthy` емес

**Шешу**: Күту немесе healthcheck параметрлерін өзгерту

### 2. DNS мәселесі

**Белгілері**: `ping redis` жұмыс істемейді

**Шешу**: 
```bash
# Сеть тексеру
docker network inspect <network_name>

# Контейнерлерді қайта іске қосу
docker compose restart
```

### 3. Порт конфликті

**Белгілері**: Порт 6379 бос емес

**Шешу**: Портты өзгерту немесе конфликтілі процесті тоқтату

### 4. Кеш мәселесі

**Белгілері**: Ескі конфигурация пайдаланылады

**Шешу**:
```bash
docker compose down
docker system prune -f
docker compose up -d --build
```

## Тестілеу скрипті

```bash
#!/bin/bash
echo "=== Redis диагностика ==="

echo "1. Redis контейнері статусы:"
docker compose ps redis

echo -e "\n2. Redis-қа қосылу:"
docker compose exec redis redis-cli ping

echo -e "\n3. Resource Service-тен Redis-қа қосылу:"
docker compose exec resource-service sh -c "timeout 5 nc -zv redis 6379" || echo "Қосылу мәселесі!"

echo -e "\n4. Environment айнымалылары:"
docker compose exec resource-service env | grep REDIS

echo -e "\n5. Redis логтары (соңғы 10 жол):"
docker compose logs redis --tail 10

echo -e "\n6. Resource Service логтары (Redis қателері):"
docker compose logs resource-service 2>&1 | grep -i redis | tail -5
```

## Қосымша ақпарат

Егер мәселелер жалғаса:
1. Docker версиясын тексеру: `docker --version` (20.10+ болуы керек)
2. Docker Compose версиясын тексеру: `docker compose version` (v2+ болуы керек)
3. Система ресурстарын тексеру: `docker system df`
4. Барлық контейнерлерді тоқтату және қайта іске қосу



