# PowerShell скрипт ожидания готовности Redis

param(
    [string]$Host = "redis",
    [int]$Port = 6379,
    [int]$Timeout = 60
)

$elapsed = 0
$interval = 1

Write-Host "Ожидание Redis на ${Host}:${Port}..."

while ($elapsed -lt $Timeout) {
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $connection = $tcpClient.BeginConnect($Host, $Port, $null, $null)
        $wait = $connection.AsyncWaitHandle.WaitOne(1000, $false)
        
        if ($wait) {
            $tcpClient.EndConnect($connection)
            $tcpClient.Close()
            Write-Host "Redis готов!"
            exit 0
        }
    } catch {
        # Соединение недоступно, продолжаем ожидание
    }
    
    Start-Sleep -Seconds $interval
    $elapsed += $interval
    Write-Host "Redis недоступен - ожидание... ($elapsed/$Timeout сек)"
}

Write-Host "Таймаут ожидания Redis!"
exit 1



