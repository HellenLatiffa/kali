$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jdkPath = $env:KALI_JDK21_HOME
if (-not $jdkPath) {
    $jdkPath = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
}

if (-not (Test-Path (Join-Path $jdkPath 'bin\java.exe'))) {
    throw "JDK 21 não encontrado em '$jdkPath'."
}

$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH
$originalEnvironment = @{}
try {
    $env:JAVA_HOME = $jdkPath
    $env:PATH = (Join-Path $jdkPath 'bin') + ';' + $env:PATH

    $envFile = Join-Path $root '.env'
    if (Test-Path $envFile) {
        Get-Content $envFile | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
                $parts = $line.Split('=', 2)
                $key = $parts[0].Trim()
                if (-not $originalEnvironment.ContainsKey($key)) {
                    $originalEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
                }
                [Environment]::SetEnvironmentVariable($key, $parts[1].Trim(), 'Process')
            }
        }
    }

    Push-Location (Join-Path $root 'backend')
    try {
        & .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) {
            throw "Backend encerrou com código $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:PATH = $originalPath
    foreach ($entry in $originalEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
    }
}
