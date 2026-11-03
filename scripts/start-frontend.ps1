$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'frontend'

if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
    throw 'Dependências do frontend ausentes. Execute npm install em frontend antes de iniciar.'
}

Push-Location $frontend
try {
    npm run dev
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend encerrou com código $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
