# Система бронирования ресурсов (Booking & Reservation System)

Распределенная микросервисная система для управления ресурсами и бронированиями с использованием Spring Boot, Keycloak, PostgreSQL, RabbitMQ, Redis и Kubernetes.

## Архитектура

### Микросервисы:
- **API Gateway** (порт 8080): Единая точка входа, маршрутизация запросов, аутентификация
- **Resource Service** (порт 8081): Управление ресурсами (комнаты, оборудование, транспорт и т.д.)
- **Booking Service** (порт 8082): Управление бронированиями ресурсов
- **Notification Service** (порт 8083): Обработка уведомлений о бронированиях через RabbitMQ

### Инфраструктура:
- **Frontend** (порт 3000): React приложение с современным UI
- **Keycloak** (порт 8088): Аутентификация и авторизация
- **PostgreSQL**: Основная база данных
- **RabbitMQ** (порты 5672, 15672): Асинхронная обработка уведомлений
- **Redis** (порт 6379): Кеширование данных
- **MinIO** (порты 9000, 9001): Хранилище файлов
- **Prometheus** (порт 9090): Сбор метрик
- **Grafana** (порт 3001): Визуализация метрик

## Функциональность

### Resource Service
- Создание, просмотр, обновление и удаление ресурсов
- Фильтрация по типу и доступности
- Проверка доступности ресурсов
- Кеширование в Redis

### Booking Service
- Создание бронирований с проверкой доступности
- Просмотр своих бронирований
- Отмена бронирований
- Автоматический расчет стоимости
- Интеграция с Resource Service для проверки доступности

### Notification Service
- Асинхронная обработка уведомлений о бронированиях
- Обработка событий подтверждения и отмены

## Фронтенд

Современный React фронтенд с красивым UI и анимациями:
- **Технологии**: React 18, TypeScript, Vite, Tailwind CSS, Framer Motion
- **Страницы**: Вход, Панель управления, Ресурсы, Бронирования
- **Особенности**: Адаптивный дизайн, плавные анимации, интеграция с Keycloak

### Запуск фронтенда локально:
```bash
cd frontend
npm install
npm run dev
```

Фронтенд будет доступен на `http://localhost:3000`

## Быстрый старт

### 1. Сборка проекта
```powershell
mvn -pl services/api-gateway,services/file-service,services/service-two,services/data-processor clean package -DskipTests
```

### 2. Сборка фронтенда (опционально, для Docker)
```powershell
cd frontend
npm install
npm run build
cd ..
```

### 3. Запуск через Docker Compose
```powershell
cd infra/docker
docker compose up -d
```

### 3. Настройка Keycloak
1. Откройте http://localhost:8088
2. Войдите как admin/admin
3. Создайте realm "app" (если не создан автоматически)
4. Создайте клиента `api-gateway`:
   - Client ID: `api-gateway`
   - Client authentication: ON
   - Client secret: `gateway-secret`
   - Direct access grants: ON
   - Service accounts roles: ON
5. Создайте пользователя `user` с паролем `password`

### 4. Проверка работы
```powershell
# Получение токена
$resp = curl.exe -s -X POST "http://localhost:8088/realms/app/protocol/openid-connect/token" -d "client_id=api-gateway" -d "client_secret=gateway-secret" -d "grant_type=client_credentials"
$token = ($resp | ConvertFrom-Json).access_token

# Проверка health
curl.exe -H "Authorization: Bearer $token" http://localhost:8080/actuator/health
```

## API Endpoints

### Resource Service (через Gateway: `/resources`)
- `GET /resources` - Список всех ресурсов
- `GET /resources?type=MEETING_ROOM&available=true` - Фильтрация ресурсов
- `GET /resources/{id}` - Получить ресурс по ID
- `POST /resources` - Создать ресурс (требуется роль admin)
- `PUT /resources/{id}` - Обновить ресурс (требуется роль admin)
- `DELETE /resources/{id}` - Удалить ресурс (требуется роль admin)
- `GET /resources/{id}/availability` - Проверить доступность ресурса

### Booking Service (через Gateway: `/bookings`)
- `GET /bookings` - Получить свои бронирования
- `GET /bookings/{id}` - Получить бронирование по ID
- `GET /bookings/resource/{resourceId}` - Получить бронирования ресурса
- `POST /bookings` - Создать бронирование
- `DELETE /bookings/{id}` - Отменить бронирование
- `GET /bookings/availability?resourceId=1&startTime=...&endTime=...` - Проверить доступность

## Примеры использования

### Создание ресурса
```json
POST /resources
{
  "name": "Конференц-зал A",
  "description": "Большой зал для встреч",
  "type": "CONFERENCE_HALL",
  "pricePerHour": 5000.00,
  "capacity": 50,
  "available": true
}
```

### Создание бронирования
```json
POST /bookings
{
  "resourceId": 1,
  "startTime": "2025-12-15T10:00:00",
  "endTime": "2025-12-15T12:00:00",
  "notes": "Встреча команды"
}
```

## Доступ к сервисам

- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Keycloak**: http://localhost:8088 (admin/admin)
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)

## Kubernetes

Манифесты находятся в `k8s/base/`:
- `infra.yaml` - инфраструктура (Keycloak, PostgreSQL, RabbitMQ, Redis, MinIO, Prometheus, Grafana)
- `apps.yaml` - микросервисы (API Gateway, Resource Service, Booking Service, Notification Service)

## Технологии

- Spring Boot 3.3.3
- Spring Cloud Gateway
- Spring Data JPA
- Spring Security OAuth2 Resource Server
- PostgreSQL 15
- RabbitMQ 3.13
- Redis 7
- MinIO
- Keycloak 24.0.5
- Prometheus, Grafana
- Docker, Kubernetes
