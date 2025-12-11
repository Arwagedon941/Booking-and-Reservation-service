# TLS Configuration

## Генерация сертификатов для разработки

### Linux/Mac:
```bash
cd k8s/tls
chmod +x generate-certs.sh
./generate-certs.sh
```

### Windows:
```powershell
cd k8s/tls
.\generate-certs.ps1
# Затем используйте OpenSSL для конвертации PFX в PEM
```

## Создание Kubernetes Secrets

После генерации сертификатов создайте Secrets:

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

## Production (Let's Encrypt)

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

Затем в Ingress используйте аннотацию:
```yaml
annotations:
  cert-manager.io/cluster-issuer: letsencrypt-prod
```

