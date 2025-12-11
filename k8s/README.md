# Kubernetes Deployment Guide

## Предварительные требования

1. Kubernetes кластер (minikube, kind, или production кластер)
2. kubectl настроен и подключен к кластеру
3. NGINX Ingress Controller установлен
4. (Опционально) cert-manager для автоматического управления TLS сертификатами

## Установка NGINX Ingress Controller

```bash
# Для minikube
minikube addons enable ingress

# Для других кластеров
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.1/deploy/static/provider/cloud/deploy.yaml
```

## Генерация TLS сертификатов

### Development (self-signed)

```bash
cd k8s/tls

# Linux/Mac
chmod +x generate-certs.sh
./generate-certs.sh

# Windows
.\generate-certs.ps1
```

### Создание Kubernetes Secrets

```bash
kubectl create namespace platform

# API Gateway TLS
kubectl create secret tls api-gateway-tls \
  --cert=certs/tls.crt \
  --key=certs/tls.key \
  -n platform

# Keycloak TLS
kubectl create secret tls keycloak-tls \
  --cert=certs/keycloak-tls.crt \
  --key=certs/keycloak-tls.key \
  -n platform
```

### Production (Let's Encrypt)

Для production используйте cert-manager:

```bash
# Установка cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Создание ClusterIssuer
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

Затем в `k8s/base/apps.yaml` раскомментируйте аннотацию:
```yaml
annotations:
  cert-manager.io/cluster-issuer: letsencrypt-prod
```

## Развертывание

### 1. Сборка Docker образов

```bash
# Сборка всех сервисов
cd services/api-gateway && docker build -t api-gateway:latest .
cd ../file-service && docker build -t resource-service:latest .
cd ../service-two && docker build -t booking-service:latest .
cd ../data-processor && docker build -t notification-service:latest .

# Для minikube нужно загрузить образы в minikube
minikube image load api-gateway:latest
minikube image load resource-service:latest
minikube image load booking-service:latest
minikube image load notification-service:latest
```

### 2. Применение манифестов

```bash
# Создание namespace и secrets
kubectl apply -f k8s/base/infra.yaml

# Развертывание приложений
kubectl apply -f k8s/base/apps.yaml

# Применение HPA для автоматического масштабирования
kubectl apply -f k8s/base/hpa.yaml

# Применение PodDisruptionBudget для высокой доступности
kubectl apply -f k8s/base/pdb.yaml
```

### 3. Проверка статуса

```bash
# Проверка подов
kubectl get pods -n platform

# Проверка сервисов
kubectl get svc -n platform

# Проверка Ingress
kubectl get ingress -n platform

# Проверка HPA
kubectl get hpa -n platform

# Логи сервиса
kubectl logs -f deployment/api-gateway -n platform
```

## Настройка /etc/hosts (для локального доступа)

Добавьте в `/etc/hosts` (Linux/Mac) или `C:\Windows\System32\drivers\etc\hosts` (Windows):

```
<INGRESS_IP> app.local
<INGRESS_IP> keycloak.app.local
```

Получить IP Ingress:
```bash
kubectl get ingress -n platform
# Или для minikube
minikube ip
```

## Доступ к приложению

- **API Gateway (HTTPS)**: https://app.local
- **Keycloak (HTTPS)**: https://keycloak.app.local
- **Grafana**: `kubectl port-forward svc/grafana 3001:3000 -n platform` → http://localhost:3001
- **Prometheus**: `kubectl port-forward svc/prometheus 9090:9090 -n platform` → http://localhost:9090

## Высокая доступность (HA)

### Автоматическое масштабирование (HPA)

HPA автоматически масштабирует поды на основе CPU и памяти:
- **API Gateway**: 2-5 реплик
- **Resource Service**: 2-5 реплик
- **Booking Service**: 2-5 реплик
- **Notification Service**: 2-3 реплики

### Pod Disruption Budget (PDB)

PDB гарантирует минимальное количество доступных подов во время обновлений:
- Минимум 1 под всегда доступен для каждого сервиса

### Health Probes

Все сервисы имеют:
- **Liveness Probe**: Проверяет, что контейнер работает
- **Readiness Probe**: Проверяет, что контейнер готов принимать трафик

## Мониторинг

### Prometheus

```bash
kubectl port-forward svc/prometheus 9090:9090 -n platform
```

### Grafana

```bash
kubectl port-forward svc/grafana 3001:3000 -n platform
# Логин: admin / admin
```

## Обновление приложения

```bash
# Пересборка образа
docker build -t api-gateway:latest -f services/api-gateway/Dockerfile services/api-gateway

# Обновление в кластере
kubectl rollout restart deployment/api-gateway -n platform

# Отслеживание обновления
kubectl rollout status deployment/api-gateway -n platform
```

## Устранение неполадок

### Проверка логов

```bash
# Логи конкретного пода
kubectl logs <pod-name> -n platform

# Логи всех подов deployment
kubectl logs -f deployment/api-gateway -n platform

# Предыдущие логи (если под перезапустился)
kubectl logs <pod-name> --previous -n platform
```

### Проверка событий

```bash
kubectl get events -n platform --sort-by='.lastTimestamp'
```

### Описание ресурса

```bash
kubectl describe pod <pod-name> -n platform
kubectl describe deployment api-gateway -n platform
```

### Проверка Ingress

```bash
kubectl describe ingress api-gateway -n platform
kubectl get ingress -n platform -o yaml
```

### Проблемы с TLS

```bash
# Проверка секрета
kubectl get secret api-gateway-tls -n platform -o yaml

# Проверка сертификата
kubectl get secret api-gateway-tls -n platform -o jsonpath='{.data.tls\.crt}' | base64 -d | openssl x509 -text -noout
```

## Масштабирование вручную

```bash
# Увеличить количество реплик
kubectl scale deployment api-gateway --replicas=5 -n platform

# Проверить текущее количество
kubectl get deployment api-gateway -n platform
```

## Очистка

```bash
# Удаление всех ресурсов
kubectl delete namespace platform

# Или по отдельности
kubectl delete -f k8s/base/apps.yaml
kubectl delete -f k8s/base/infra.yaml
kubectl delete -f k8s/base/hpa.yaml
kubectl delete -f k8s/base/pdb.yaml
```

