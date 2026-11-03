$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
$helperModule = Join-Path $PSScriptRoot 'PostgresProvisioning.Helpers.psm1'

if (Test-Path $envFile) {
    throw 'O arquivo .env já existe. Nenhuma alteração foi feita; revise-o manualmente.'
}
if (-not (Test-Path $helperModule)) {
    throw "Helper de configuração não encontrado em '$helperModule'."
}

Import-Module $helperModule -Force

$appSecure = $null
$migrationSecure = $null
$appPassword = $null
$migrationPassword = $null
$fileCreated = $false

try {
    $appSecure = Read-Host 'Senha existente de kali_app (entrada protegida)' -AsSecureString
    $migrationSecure = Read-Host 'Senha existente de kali_migrator (entrada protegida)' -AsSecureString
    $appPassword = ConvertTo-KaliPlainText $appSecure
    $migrationPassword = ConvertTo-KaliPlainText $migrationSecure

    if (-not (Test-KaliRolePassword $appPassword $migrationPassword)) {
        throw 'Credenciais incompatíveis com o formato local .env; nenhuma credencial foi gravada. Use senhas distintas com 24+ caracteres ASCII imprimíveis, sem espaços, barra invertida, ${, ou começando por # ou !.'
    }

    $envContents = @"
DB_URL=jdbc:postgresql://127.0.0.1:5432/kali_db
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=kali_db
DB_USER=kali_app
DB_PASSWORD=$appPassword
DB_MIGRATION_USER=kali_migrator
DB_MIGRATION_PASSWORD=$migrationPassword
COOKIE_SECURE=false
HEALTH_CONSENT_VERSION=2026-01
"@
    [IO.File]::WriteAllText($envFile, $envContents + "`r`n", (New-Object Text.UTF8Encoding($false)))
    $fileCreated = $true

    $acl = Get-Acl $envFile
    $acl.SetAccessRuleProtection($true, $false)
    $currentSid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $accessRule = New-Object System.Security.AccessControl.FileSystemAccessRule `
        -ArgumentList $currentSid, 'FullControl', 'Allow'
    $acl.SetAccessRule($accessRule)
    Set-Acl -Path $envFile -AclObject $acl

    Write-Output 'Arquivo .env criado e ACL restrita ao usuário atual. Credenciais não foram exibidas.'
} catch {
    if ($fileCreated -and (Test-Path $envFile)) {
        Remove-Item -LiteralPath $envFile -Force
    }
    throw
} finally {
    $appPassword = $null
    $migrationPassword = $null
    $appSecure = $null
    $migrationSecure = $null
}
