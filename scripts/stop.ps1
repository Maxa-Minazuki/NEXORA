# Остановка сайта JYLY одним кликом (вызывается из stop.bat).
$Host.UI.RawUI.WindowTitle = 'JYLY - остановка'
$root = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $root 'server.pid'

function Test-Running {
    [bool](Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
}

# Окно сервера, открытое через start.bat, закрываем вместе с дочерними процессами.
if (Test-Path $pidFile) {
    $serverPid = [int](Get-Content $pidFile)
    if (Get-Process -Id $serverPid -ErrorAction SilentlyContinue | Where-Object { $_.Name -eq 'cmd' }) {
        taskkill /pid $serverPid /t /f | Out-Null
    }
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}

# На случай запуска другим способом (VS Code, терминал, java -jar).
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like '*kz.jyly*' } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue } |
    Where-Object { $_.Name -eq 'java' } |
    Stop-Process -Force -ErrorAction SilentlyContinue

Start-Sleep -Seconds 1
if (Test-Running) {
    Write-Host 'Порт 8080 всё ещё занят другой программой.' -ForegroundColor Red
    Read-Host 'Нажмите Enter, чтобы закрыть окно' | Out-Null
    exit 1
}
Write-Host 'Сайт выключен.' -ForegroundColor Green
Start-Sleep -Seconds 3
