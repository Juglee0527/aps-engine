param(
    [int]$ServerPort = 0
)

$ErrorActionPreference = "Stop"
& chcp.com 65001 | Out-Null
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $repositoryRoot ".env"
$environmentExampleFile = Join-Path $repositoryRoot ".env.example"

if (-not (Test-Path -LiteralPath $environmentFile)) {
    Copy-Item -LiteralPath $environmentExampleFile `
        -Destination $environmentFile
    Write-Host "[APS] .env 파일을 생성했습니다."
}

Get-Content -LiteralPath $environmentFile |
    Where-Object { $_ -match "^[A-Z_][A-Z0-9_]*=" } |
    ForEach-Object {
        $separatorIndex = $_.IndexOf("=")
        $variableName = $_.Substring(0, $separatorIndex)
        $variableValue = $_.Substring($separatorIndex + 1)
        [Environment]::SetEnvironmentVariable(
            $variableName,
            $variableValue,
            "Process"
        )
    }

if ([string]::IsNullOrWhiteSpace($env:POSTGRES_PASSWORD)) {
    throw ".env의 POSTGRES_PASSWORD를 설정해 주세요."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker 명령을 찾을 수 없습니다. Docker Desktop을 설치해 주세요."
}

function Test-TcpPortInUse {
    param(
        [int]$Port
    )

    $activeListeners = [System.Net.NetworkInformation.IPGlobalProperties]::
        GetIPGlobalProperties().GetActiveTcpListeners()
    return $null -ne ($activeListeners |
        Where-Object { $_.Port -eq $Port } |
        Select-Object -First 1)
}

function Resolve-ServerPort {
    param(
        [int]$RequestedPort
    )

    $preferredPort = $RequestedPort
    if ($preferredPort -le 0) {
        $configuredPort = 0
        if (-not [string]::IsNullOrWhiteSpace($env:SERVER_PORT) -and
            [int]::TryParse($env:SERVER_PORT, [ref]$configuredPort)) {
            $preferredPort = $configuredPort
        } else {
            $preferredPort = 8080
        }
    }

    for ($candidatePort = $preferredPort;
         $candidatePort -le $preferredPort + 10;
         $candidatePort++) {
        if (-not (Test-TcpPortInUse -Port $candidatePort)) {
            return $candidatePort
        }
    }

    throw "$preferredPort 번부터 연속된 11개 포트가 모두 사용 중입니다."
}

$env:GRADLE_USER_HOME = Join-Path $repositoryRoot ".gradle-user-home"

Push-Location $repositoryRoot
try {
    & docker info --format "{{.ServerVersion}}" *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Desktop이 실행 중인지 확인해 주세요."
    }

    Write-Host "[APS] PostgreSQL을 시작합니다."
    & docker compose up -d postgres
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL 컨테이너를 시작하지 못했습니다."
    }

    $databaseContainerId = (
        & docker compose ps -q postgres
    ).Trim()
    if ([string]::IsNullOrWhiteSpace($databaseContainerId)) {
        throw "PostgreSQL 컨테이너를 찾지 못했습니다."
    }

    Write-Host "[APS] PostgreSQL 준비를 기다립니다."
    $databaseReadyDeadline = (Get-Date).AddSeconds(60)
    do {
        $databaseHealth = (
            & docker inspect `
                --format "{{.State.Health.Status}}" `
                $databaseContainerId
        ).Trim()

        if ($databaseHealth -eq "healthy") {
            break
        }
        if ($databaseHealth -eq "unhealthy") {
            throw "PostgreSQL 상태가 unhealthy입니다. 'docker compose logs postgres'로 로그를 확인해 주세요."
        }

        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $databaseReadyDeadline)

    if ($databaseHealth -ne "healthy") {
        throw "60초 안에 PostgreSQL이 준비되지 않았습니다."
    }

    $selectedServerPort = Resolve-ServerPort -RequestedPort $ServerPort
    if ($selectedServerPort -ne 8080) {
        Write-Host "[APS] 서버 포트로 $selectedServerPort 포트를 사용합니다."
    }
    $env:SERVER_PORT = [string]$selectedServerPort

    Write-Host "[APS] 서버 주소: http://localhost:$selectedServerPort"
    Write-Host "[APS] 서버를 종료하려면 Ctrl+C를 누르세요."
    & ".\gradlew.bat" bootRun "--args=--spring.profiles.active=local"
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
