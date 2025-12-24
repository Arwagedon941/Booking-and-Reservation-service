# PowerShell скрипт для генерации self-signed сертификатов (Windows)
# Для production используйте cert-manager с Let's Encrypt

$DOMAIN = "app.local"
$DAYS = 365

# Создаем директорию для сертификатов
New-Item -ItemType Directory -Force -Path "certs" | Out-Null

# Генерируем приватный ключ и сертификат для API Gateway
$cert = New-SelfSignedCertificate `
    -DnsName $DOMAIN, "*.$DOMAIN" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -KeyAlgorithm RSA `
    -KeyLength 2048 `
    -NotAfter (Get-Date).AddDays($DAYS)

# Экспортируем сертификат в PEM формат
$certPath = "Cert:\CurrentUser\My\$($cert.Thumbprint)"
$pwd = ConvertTo-SecureString -String "temp" -Force -AsPlainText
Export-PfxCertificate -Cert $certPath -FilePath "certs\temp.pfx" -Password $pwd | Out-Null

# Конвертируем PFX в PEM (требуется OpenSSL)
# Если OpenSSL не установлен, используйте онлайн конвертер или установите через chocolatey: choco install openssl
Write-Host "Сертификат создан: $($cert.Thumbprint)"
Write-Host "Для экспорта в PEM формат используйте OpenSSL:"
Write-Host "openssl pkcs12 -in certs/temp.pfx -out certs/tls.crt -clcerts -nokeys -passin pass:temp"
Write-Host "openssl pkcs12 -in certs/temp.pfx -out certs/tls.key -nocerts -nodes -passin pass:temp"

Write-Host "`nДля создания Kubernetes Secret выполните:"
Write-Host "kubectl create secret tls api-gateway-tls --cert=certs/tls.crt --key=certs/tls.key -n platform"




