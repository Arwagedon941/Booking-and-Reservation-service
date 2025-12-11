# Скрипт для исправления пользователя в Keycloak
$adminToken = (curl.exe -s -X POST "http://localhost:8088/realms/master/protocol/openid-connect/token" `
    -d "client_id=admin-cli" `
    -d "username=admin" `
    -d "password=admin" `
    -d "grant_type=password" | ConvertFrom-Json).access_token

# Получаем ID пользователя
$users = curl.exe -s -X GET "http://localhost:8088/admin/realms/app/users?username=user" `
    -H "Authorization: Bearer $adminToken" | ConvertFrom-Json

if ($users.Count -eq 0) {
    Write-Host "Пользователь не найден, создаем нового..."
    $userBody = @{
        username = "user"
        email = "user@example.com"
        emailVerified = $true
        enabled = $true
        requiredActions = @()
        credentials = @(@{
            type = "password"
            value = "password"
            temporary = $false
        })
    } | ConvertTo-Json -Depth 10
    
    curl.exe -s -X POST "http://localhost:8088/admin/realms/app/users" `
        -H "Authorization: Bearer $adminToken" `
        -H "Content-Type: application/json" `
        -d $userBody | Out-Null
    Write-Host "Пользователь создан"
} else {
    $userId = $users[0].id
    Write-Host "Обновляем пользователя $userId"
    
    # Обновляем пользователя
    $updateBody = @{
        requiredActions = @()
        emailVerified = $true
        enabled = $true
    } | ConvertTo-Json -Depth 10
    
    curl.exe -s -X PUT "http://localhost:8088/admin/realms/app/users/$userId" `
        -H "Authorization: Bearer $adminToken" `
        -H "Content-Type: application/json" `
        -d $updateBody | Out-Null
    
    # Устанавливаем пароль
    $passwordBody = @{
        type = "password"
        value = "password"
        temporary = $false
    } | ConvertTo-Json -Depth 10
    
    curl.exe -s -X PUT "http://localhost:8088/admin/realms/app/users/$userId/reset-password" `
        -H "Authorization: Bearer $adminToken" `
        -H "Content-Type: application/json" `
        -d $passwordBody | Out-Null
    
    Write-Host "Пользователь обновлен"
}

Write-Host "`nПроверка входа..."
Start-Sleep -Seconds 2
$testResponse = curl.exe -s -X POST "http://localhost:8080/auth/realms/app/protocol/openid-connect/token" `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "client_id=api-gateway&client_secret=gateway-secret&grant_type=password&username=user&password=password"

if ($testResponse -match "access_token") {
    Write-Host "SUCCESS: Login works!"
} else {
    Write-Host "ERROR: Login failed: $testResponse"
}

