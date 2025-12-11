# Скрипт для тестирования аутентификации
Write-Host "=== Testing Authentication ==="

# 1. Получаем токен
Write-Host "`n1. Getting token..."
$tokenResponse = curl.exe -s -X POST "http://localhost:8080/auth/realms/app/protocol/openid-connect/token" `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "client_id=api-gateway&client_secret=gateway-secret&grant_type=password&username=user&password=password"

Write-Host "Token response: $tokenResponse"

$tokenJson = $tokenResponse | ConvertFrom-Json
if ($tokenJson.access_token) {
    $token = $tokenJson.access_token
    Write-Host "SUCCESS: Token obtained, length: $($token.Length)"
    Write-Host "Token preview: $($token.Substring(0, [Math]::Min(50, $token.Length)))..."
    
    # 2. Тестируем запрос с токеном
    Write-Host "`n2. Testing request with token..."
    $resourceResponse = curl.exe -v -X GET "http://localhost:8080/resources" `
        -H "Authorization: Bearer $token" 2>&1
    
    Write-Host "`nResponse:"
    $resourceResponse | Select-String -Pattern "HTTP|401|403|200|error" | Select-Object -First 10
    
    if ($resourceResponse -match "200") {
        Write-Host "`nSUCCESS: Request works!"
    } else {
        Write-Host "`nERROR: Request failed"
    }
} else {
    Write-Host "ERROR: Failed to get token"
    Write-Host "Response: $tokenResponse"
}

Write-Host "`n=== Check API Gateway logs ==="
Write-Host "docker logs docker-api-gateway-1 --tail 50"




