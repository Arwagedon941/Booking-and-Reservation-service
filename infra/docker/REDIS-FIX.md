# Redis қосылым мәселесін шешу

## Мәселе

`RedisConnectionFailureException: Unable to connect to Redis` қатесі пайда болғанда, бұл Redis контейнерінің дайын болмағанын немесе қосылу мәселесін көрсетеді.

## Шешу

### 1. Контейнерлерді тоқтату және қайта іске қосу

```bash
cd infra/docker
docker compose down
docker compose up -d
```

### 2. Redis контейнерінің жұмыс істеп тұрғанын тексеру

```bash
docker compose ps redis
```

Статус `Up` болуы керек.

### 3. Redis-қа тікелей қосылуды тексеру

```bash
docker compose exec redis redis-cli ping
```

Жауап `PONG` болуы керек.

### 4. Логтарды тексеру

```bash
# Redis логтары
docker compose logs redis

# Resource Service логтары
docker compose logs resource-service

# Booking Service логтары
docker compose logs booking-service
```

### 5. Егер мәселе жалғаса

#### Вариант A: Контейнерлерді толығымен қайта іске қосу

```bash
docker compose down -v  # Барлық контейнерлер мен volume-дарды жою
docker compose up -d --build  # Қайта билдлеу және іске қосу
```

#### Вариант B: Redis-ты бөлек іске қосу

```bash
# Redis-ты бөлек іске қосу
docker compose up -d redis

# 5 секунд күту
sleep 5

# Басқа сервистерді іске қосу
docker compose up -d
```

#### Вариант C: Портты тексеру

Егер 6379 порты бос емес болса:

```bash
# Windows
netstat -ano | findstr :6379

# Linux/Mac
lsof -i :6379
```

Егер порт бос емес болса, `docker-compose.yml` файлында портты өзгертуге болады:

```yaml
redis:
  ports:
    - "6380:6379"  # 6379 орнына 6380
```

Содан кейін `REDIS_PORT` environment айнымалысын да өзгерту керек.

## Не істелді

1. ✅ Redis-қа healthcheck қосылды
2. ✅ Сервистер Redis дайын болғанға дейін күтеді (`condition: service_healthy`)
3. ✅ `restart: on-failure` қосылды - сервистер қате кезінде автоматты түрде қайта іске қосылады
4. ✅ Redis `--bind 0.0.0.0` командасымен іске қосылады (барлық интерфейстерде тыңдайды)

## Тестілеу

Барлық сервистер іске қосылғаннан кейін:

```bash
# Redis-ты тексеру
docker compose exec redis redis-cli ping

# Resource Service health check
curl http://localhost:8081/actuator/health

# Booking Service health check
curl http://localhost:8082/actuator/health
```

## Ескерту

Егер сіз ескі `docker-compose.yml` файлын пайдалансаңыз, жаңа версияны Git-тен тартыңыз:

```bash
git pull
cd infra/docker
docker compose down
docker compose up -d
```

