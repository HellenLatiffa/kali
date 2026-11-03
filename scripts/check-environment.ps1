$ErrorActionPreference = 'Stop'

$jdkPath = $env:KALI_JDK21_HOME
if (-not $jdkPath) {
    $jdkPath = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
}
if (-not (Test-Path (Join-Path $jdkPath 'bin\java.exe'))) {
    throw "JDK 21 não encontrado em '$jdkPath'. Ajuste o caminho em .vscode/settings.json e nos scripts do projeto."
}

Write-Output 'JDK do projeto: Temurin 21 disponível.'

foreach ($tool in @('node', 'npm', 'git')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Ferramenta obrigatória não encontrada: $tool."
    }
    Write-Output "${tool}: disponível."
}

if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Output 'Docker: disponível.'
} else {
    Write-Output 'Docker: não encontrado; Docker Compose é opcional.'
}

$postgresService = Get-Service -Name 'postgresql-x64-17' -ErrorAction SilentlyContinue
if ($postgresService) {
    Write-Output ("PostgreSQL: serviço {0} ({1})." -f $postgresService.Name, $postgresService.Status)
} else {
    Write-Output 'PostgreSQL: serviço conhecido não encontrado; verifique uma instalação local ou use Compose.'
}

$pgIsReady = 'C:\Program Files\PostgreSQL\17\bin\pg_isready.exe'
if (Test-Path $pgIsReady) {
    & $pgIsReady -h 127.0.0.1 -p 5432
    if ($LASTEXITCODE -ne 0) {
        throw 'O servidor PostgreSQL local não aceitou conexões na porta 5432.'
    }
} else {
    Write-Output 'pg_isready não foi encontrado no caminho padrão da instalação identificada.'
}

if (-not (Test-Path (Join-Path $PSScriptRoot '..\backend\mvnw.cmd'))) {
    throw 'Maven Wrapper do backend ainda não foi configurado.'
}
Write-Output 'Maven: será executado por backend\mvnw.cmd.'
