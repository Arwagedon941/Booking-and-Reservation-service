# Redis қосылым мәселелерін шешу

## Мәселе

`RedisConnectionFailureException: Unable to connect to Redis` қатесі пайда болғанда, бұл Redis контейнерінің дайын болмағанын немесе қосылу мәселесін көрсетеді.

## Жедел шешу

### 1. Контейнерлерді тоқтату және қайта іске қосу

```bash
cd infra/docker
docker compose down
docker compose up -d
```

### 2. Redis статусын тексеру

```bash
# Redis контейнерінің жұмыс істеп тұрғанын тексеру
docker compose ps redis

# Redis-қа тікелей қосылу
docker compose exec redis redis-cli ping
# Жауап: PONG болуы керек
```

### 3. Логтарды тексеру

```bash
# Redis логтары
docker compose logs redis

# Resource Service логтары
docker compose logs resource-service | grep -i redis

# Booking Service логтары
docker compose logs booking-service | grep -i redis
```

## Толық шешу

### Вариант A: Толық қайта іске қосу

```bash
cd infra/docker
docker compose down -v  # Барлық контейнерлер мен volume-дарды жою
docker compose up -d --build  # Қайта билдлеу және іске қосу
```

### Вариант B: Пошаговый запуск

```bash
# 1. Redis-ты бірінші іске қосу
docker compose up -d redis

# 2. Redis-тың дайын болуын күту (10-15 секунд)
sleep 15  # Linux/Mac
# Windows PowerShell: Start-Sleep -Seconds 15

# 3. Redis-ты тексеру
docker compose exec redis redis-cli ping

# 4. Басқа сервистерді іске қосу
docker compose up -d
```

### Вариант C: Портты тексеру

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

Содан кейін environment айнымалысын да өзгерту керек:
```yaml
REDIS_PORT: 6380
```

## Не істелді

1. ✅ Redis-қа healthcheck қосылды (10 секунд start_period)
2. ✅ Redis `--protected-mode no` режимінде іске қосылады
3. ✅ Сервистер Redis дайын болғанға дейін күтеді (`condition: service_healthy`)
4. ✅ Redis-қа retry механизмі қосылды (auto-reconnect)
5. ✅ Connection timeout 10 секундқа орнатылды
6. ✅ `restart: on-failure` қосылды - сервистер қате кезінде автоматты түрде қайта іске қосылады

## Тестілеу

Барлық сервистер іске қосылғаннан кейін:

```bash
# 1. Redis-ты тексеру
docker compose exec redis redis-cli ping
# Жауап: PONG

# 2. Resource Service health check
curl http://localhost:8081/actuator/health

# 3. Booking Service health check
curl http://localhost:8082/actuator/health

# 4. Барлық контейнерлердің статусын тексеру
docker compose ps
```

## Ескертулер

1. **Жаңа кодты тарту**: Егер сіз ескі кодты пайдалансаңыз, жаңа версияны Git-тен тартыңыз:
   ```bash
   git pull
   cd infra/docker
   docker compose down
   docker compose up -d --build
   ```

2. **Билдлеу**: Код өзгергеннен кейін сервистерді қайта билдлеу керек:
   ```bash
   docker compose up -d --build resource-service booking-service
   ```

3. **Кеш**: Егер мәселелер жалғаса, Docker кешін тазалаңыз:
   ```bash
   docker compose down
   docker system prune -f
   docker compose up -d --build
   ```

## Қосымша диагностика

### Redis контейнері іске қосылмайды

```bash
# Redis логтарын толық көру
docker compose logs redis --tail 50

# Redis контейнерін қолмен іске қосу
docker compose run --rm redis redis-cli ping
```

### Сеть мәселелері

```bash
# Resource Service контейнерінен Redis-қа қосылуды тексеру
docker compose exec resource-service ping redis

# Redis портын тексеру
docker compose exec redis netstat -tuln | grep 6379
```

### Environment айнымалыларын тексеру

```bash
# Resource Service environment айнымалыларын көру
docker compose exec resource-service env | grep REDIS

# Booking Service environment айнымалыларын көру
docker compose exec booking-service env | grep REDIS
```

## Кеңес

Егер мәселелер жалғаса:
1. Барлық контейнерлерді тоқтатыңыз: `docker compose down`
2. Docker кешін тазалаңыз: `docker system prune -f`
3. Толық қайта іске қосыңыз: `docker compose up -d --build`
4. 30 секунд күтіңіз және логтарды тексеріңіз



