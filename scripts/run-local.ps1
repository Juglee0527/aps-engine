$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $repositoryRoot ".env"

if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw ".env 파일이 없습니다. .env.example을 복사해 로컬 설정을 작성해 주세요."
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

$env:GRADLE_USER_HOME = Join-Path $repositoryRoot ".gradle-user-home"

Push-Location $repositoryRoot
try {
    & ".\gradlew.bat" bootRun "--args=--spring.profiles.active=local"
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
