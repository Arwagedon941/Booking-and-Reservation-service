# Быстрый старт: TLS / Ingress / HA

## Шаг 1: Установка NGINX Ingress Controller

```bash
# Для minikube
minikube addons enable ingress

# Для других кластеров
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
```

## Шаг 2: Генерация TLS сертификатов

```bash
cd k8s/tls

# Linux/Mac
chmod +x generate-certs.sh && ./generate-certs.sh

# Windows
.\generate-certs.ps1
```

## Шаг 3: Создание TLS Secrets

```bash
kubectl create namespace platform

kubectl create secret tls api-gateway-tls \
  --cert=certs/tls.crt \
  --key=certs/tls.key \
  -n platform

kubectl create secret tls keycloak-tls \
  --cert=certs/keycloak-tls.crt \
  --key=certs/keycloak-tls.key \
  -n platform
```

## Шаг 4: Сборка и загрузка Docker образов

```bash
# Сборка образов
docker build -t api-gateway:latest -f services/api-gateway/Dockerfile services/api-gateway
docker build -t resource-service:latest -f services/file-service/Dockerfile services/file-service
docker build -t booking-service:latest -f services/service-two/Dockerfile services/service-two
docker build -t notification-service:latest -f services/data-processor/Dockerfile services/data-processor

# Для minikube - загрузка образов
minikube image load api-gateway:latest
minikube image load resource-service:latest
minikube image load booking-service:latest
minikube image load notification-service:latest
```

## Шаг 5: Развертывание

```bash
# Инфраструктура
kubectl apply -f k8s/base/infra.yaml

# Приложения
kubectl apply -f k8s/base/apps.yaml

# HPA и PDB
kubectl apply -f k8s/base/hpa.yaml
kubectl apply -f k8s/base/pdb.yaml
```

## Шаг 6: Настройка /etc/hosts

Добавьте в `/etc/hosts` (Linux/Mac) или `C:\Windows\System32\drivers\etc\hosts` (Windows):

```
<INGRESS_IP> app.local
<INGRESS_IP> keycloak.app.local
```

Получить IP:
```bash
# Для minikube
minikube ip

# Для других кластеров
kubectl get ingress -n platform -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}'
```

## Шаг 7: Проверка

```bash
# Проверка подов
kubectl get pods -n platform

# Проверка сервисов
kubectl get svc -n platform

# Проверка Ingress
kubectl get ingress -n platform

# Проверка HPA
kubectl get hpa -n platform

# Доступ к приложению
curl -k https://app.local/actuator/health
```

## Что было настроено

✅ **TLS**: Self-signed сертификаты для dev, готовность к Let's Encrypt для prod  
✅ **Ingress**: NGINX Ingress с TLS termination и автоматическим HTTPS redirect  
✅ **HA**: 
   - 2+ реплики для всех основных сервисов
   - Health probes (liveness/readiness)
   - HPA для автоматического масштабирования
   - PodDisruptionBudget для гарантированной доступности
   - Resource limits для стабильности

## Доступ

- **API Gateway (HTTPS)**: https://app.local
- **Keycloak (HTTPS)**: https://keycloak.app.local
- **Grafana**: `kubectl port-forward svc/grafana 3001:3000 -n platform`
- **Prometheus**: `kubectl port-forward svc/prometheus 9090:9090 -n platform`

## Устранение неполадок

```bash
# Логи
kubectl logs -f deployment/api-gateway -n platform

# События
kubectl get events -n platform --sort-by='.lastTimestamp'

# Описание пода
kubectl describe pod <pod-name> -n platform
```

Подробнее: см. `k8s/README.md`


