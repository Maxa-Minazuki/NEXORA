# Запуск сайта JYLY одним кликом (вызывается из start.bat).
$Host.UI.RawUI.WindowTitle = 'JYLY - запуск'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$url = 'http://localhost:8080'
$log = Join-Path $root 'server.log'
$pidFile = Join-Path $root 'server.pid'

function Test-Running {
    [bool](Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
}

function Exit-WithPause($code) {
    Read-Host 'Нажмите Enter, чтобы закрыть окно' | Out-Null
    exit $code
}

if (Test-Running) {
    Write-Host "Сайт уже работает: $url"
    Start-Process $url
    Start-Sleep -Seconds 3
    exit 0
}

# Ищем Java: JAVA_HOME, PATH или стандартные папки установки.
$javaHomeOk = $env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))
if (-not $javaHomeOk) {
    $env:JAVA_HOME = $null
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        $jdk = @(
            "$env:USERPROFILE\.jdks",
            "$env:ProgramFiles\Eclipse Adoptium",
            "$env:ProgramFiles\Java",
            "$env:ProgramFiles\Microsoft",
            "$env:ProgramFiles\Zulu",
            "$env:ProgramFiles\Amazon Corretto"
        ) | Where-Object { Test-Path $_ } |
            ForEach-Object { Get-ChildItem $_ -Directory } |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
            Select-Object -First 1
        if (-not $jdk) {
            Write-Host 'Java не найдена.' -ForegroundColor Red
            Write-Host 'Установите Java 21 или новее: https://adoptium.net/'
            Write-Host 'При установке отметьте "Set JAVA_HOME" и "Add to PATH".'
            Exit-WithPause 1
        }
        $env:JAVA_HOME = $jdk.FullName
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    }
}

Write-Host 'Запускаю сайт... Первый запуск может занять несколько минут.'
Write-Host "Подробный лог: $log"
$mvnw = Join-Path $root 'mvnw.cmd'
$server = Start-Process cmd -ArgumentList "/c `"`"$mvnw`" spring-boot:run > server.log 2>&1`"" `
    -WorkingDirectory $root -WindowStyle Minimized -PassThru
Set-Content -Path $pidFile -Value $server.Id

# Ждём, пока сервер начнёт принимать соединения (до 10 минут).
$deadline = (Get-Date).AddMinutes(10)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if (Test-Running) {
        Write-Host ''
        Write-Host "Сайт запущен: $url" -ForegroundColor Green
        Write-Host 'Чтобы выключить, запустите stop.bat'
        Start-Process $url
        Start-Sleep -Seconds 5
        exit 0
    }
    if ($server.HasExited) { break }
}

Write-Host ''
Write-Host 'Не удалось запустить сайт. Последние строки лога:' -ForegroundColor Red
Write-Host ('-' * 60)
if (Test-Path $log) { Get-Content $log -Tail 30 -Encoding Oem }
Write-Host ('-' * 60)
Remove-Item $pidFile -ErrorAction SilentlyContinue
Exit-WithPause 1
