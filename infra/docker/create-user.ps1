# Скрипт для создания пользователя в Keycloak
Write-Host "Creating user in Keycloak..."

$adminToken = (curl.exe -s -X POST "http://localhost:8088/realms/master/protocol/openid-connect/token" `
    -d "client_id=admin-cli" `
    -d "username=admin" `
    -d "password=admin" `
    -d "grant_type=password" | ConvertFrom-Json).access_token

if (-not $adminToken) {
    Write-Host "ERROR: Failed to get admin token"
    exit 1
}

Write-Host "Admin token obtained"

# Проверяем, существует ли пользователь
$users = curl.exe -s -X GET "http://localhost:8088/admin/realms/app/users?username=user" `
    -H "Authorization: Bearer $adminToken" | ConvertFrom-Json

if ($users.Count -gt 0) {
    $userId = $users[0].id
    Write-Host "User exists, deleting..."
    curl.exe -s -X DELETE "http://localhost:8088/admin/realms/app/users/$userId" `
        -H "Authorization: Bearer $adminToken" | Out-Null
}

# Создаем нового пользователя
Write-Host "Creating new user..."
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

$createResponse = curl.exe -s -X POST "http://localhost:8088/admin/realms/app/users" `
    -H "Authorization: Bearer $adminToken" `
    -H "Content-Type: application/json" `
    -d $userBody

if ($LASTEXITCODE -eq 0 -or $createResponse -eq "") {
    Write-Host "User created successfully"
    
    # Получаем ID пользователя
    Start-Sleep -Seconds 1
    $users = curl.exe -s -X GET "http://localhost:8088/admin/realms/app/users?username=user" `
        -H "Authorization: Bearer $adminToken" | ConvertFrom-Json
    
    if ($users.Count -gt 0) {
        $userId = $users[0].id
        
        # Устанавливаем пароль отдельно
        Write-Host "Setting password..."
        $passwordBody = @{
            type = "password"
            value = "password"
            temporary = $false
        } | ConvertTo-Json -Depth 10
        
        curl.exe -s -X PUT "http://localhost:8088/admin/realms/app/users/$userId/reset-password" `
            -H "Authorization: Bearer $adminToken" `
            -H "Content-Type: application/json" `
            -d $passwordBody | Out-Null
        
        # Убеждаемся, что requiredActions пуст
        curl.exe -s -X PUT "http://localhost:8088/admin/realms/app/users/$userId" `
            -H "Authorization: Bearer $adminToken" `
            -H "Content-Type: application/json" `
            -d '{"requiredActions":[],"emailVerified":true,"enabled":true}' | Out-Null
        
        Write-Host "User configured successfully"
        
        # Тестируем вход
        Start-Sleep -Seconds 2
        Write-Host "Testing login..."
        $testResponse = curl.exe -s -X POST "http://localhost:8088/realms/app/protocol/openid-connect/token" `
            -H "Content-Type: application/x-www-form-urlencoded" `
            -d "client_id=api-gateway&client_secret=gateway-secret&grant_type=password&username=user&password=password"
        
        if ($testResponse -match "access_token") {
            Write-Host "SUCCESS: Login works!"
        } else {
            Write-Host "WARNING: Login test failed: $testResponse"
            Write-Host "Please create user manually in Keycloak UI: http://localhost:8088"
            Write-Host "Username: user, Password: password"
        }
    } else {
        Write-Host "ERROR: User not found after creation"
    }
} else {
    Write-Host "ERROR: Failed to create user: $createResponse"
    Write-Host "Please create user manually in Keycloak UI: http://localhost:8088"
}




