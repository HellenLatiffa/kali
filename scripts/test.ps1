param(
    [switch]$SkipFrontend
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot 'test-provision-postgres.ps1')

$jdkPath = $env:KALI_JDK21_HOME
if (-not $jdkPath) {
    $jdkPath = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
}

if (-not (Test-Path (Join-Path $jdkPath 'bin\java.exe'))) {
    throw "JDK 21 não encontrado em '$jdkPath'."
}

$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH
try {
    $env:JAVA_HOME = $jdkPath
    $env:PATH = (Join-Path $jdkPath 'bin') + ';' + $env:PATH

    Push-Location (Join-Path $root 'backend')
    try {
        & .\mvnw.cmd --no-transfer-progress clean verify
        if ($LASTEXITCODE -ne 0) {
            throw "Backend falhou com código $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }

    if (-not $SkipFrontend) {
        Push-Location (Join-Path $root 'frontend')
        try {
            npm run lint
            if ($LASTEXITCODE -ne 0) {
                throw "Lint do frontend falhou com código $LASTEXITCODE."
            }
            npm run build
            if ($LASTEXITCODE -ne 0) {
                throw "Build do frontend falhou com código $LASTEXITCODE."
            }
        } finally {
            Pop-Location
        }
    }
} finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:PATH = $originalPath
}
