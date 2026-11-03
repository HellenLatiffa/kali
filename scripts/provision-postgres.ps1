$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
$psql = 'C:\Program Files\PostgreSQL\17\bin\psql.exe'
$helperModule = Join-Path $PSScriptRoot 'PostgresProvisioning.Helpers.psm1'

if (-not (Test-Path $helperModule)) {
    throw "Helper de provisionamento não encontrado em '$helperModule'."
}
Import-Module $helperModule -Force

if (-not (Test-Path $psql)) {
    throw "psql não encontrado em '$psql'."
}
if (Test-Path $envFile) {
    throw 'O arquivo .env já existe. Nenhum comando administrativo foi executado; revise-o manualmente antes de continuar.'
}

function Invoke-Psql([string]$Database, [string]$InputText, [string]$AdminPassword) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $psql
    $startInfo.Arguments = "-X -h 127.0.0.1 -p 5432 -U postgres -d $Database -v ON_ERROR_STOP=1 -At"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.EnvironmentVariables['PGPASSWORD'] = $AdminPassword

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    $null = $process.Start()
    $process.StandardInput.Write($InputText)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw "psql falhou (código $($process.ExitCode)): $($stderr.Trim())"
    }
    return $stdout.Trim()
}

$adminSecure = $null
$appSecure = $null
$migrationSecure = $null
$adminPassword = $null
$appPassword = $null
$migrationPassword = $null

try {
    $adminSecure = Read-Host 'Senha da role administrativa postgres (entrada protegida)' -AsSecureString
    $adminPassword = ConvertTo-KaliPlainText $adminSecure

    $stateSql = @'
SELECT 'role:kali_app=' || EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kali_app')
UNION ALL
SELECT 'role:kali_migrator=' || EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'kali_migrator')
UNION ALL
SELECT 'database:kali_db=' || EXISTS (SELECT 1 FROM pg_database WHERE datname = 'kali_db');
'@
    $state = Invoke-Psql 'postgres' $stateSql $adminPassword
    Write-Output 'Consulta de conflito (somente leitura):'
    Write-Output $state
    if ($state -notmatch 'role:kali_app=false' -or
        $state -notmatch 'role:kali_migrator=false' -or
        $state -notmatch 'database:kali_db=false') {
        throw 'Pelo menos um nome-alvo já existe. Nenhuma role/banco foi alterado; solicite revisão antes de prosseguir.'
    }

    $appSecure = Read-Host 'Defina a senha local de kali_app (mínimo 24 caracteres: letras, números, _ ou -)' -AsSecureString
    $migrationSecure = Read-Host 'Defina uma senha diferente para kali_migrator (mesmas regras)' -AsSecureString
    $appPassword = ConvertTo-KaliPlainText $appSecure
    $migrationPassword = ConvertTo-KaliPlainText $migrationSecure
    if (-not (Test-KaliRolePassword $appPassword $migrationPassword)) {
        throw 'Senhas inválidas: use senhas diferentes, com pelo menos 24 caracteres ASCII imprimíveis, sem espaços, barra invertida ou o trecho ${.'
    }

    $createSql = @'
CREATE ROLE kali_migrator LOGIN;
CREATE ROLE kali_app LOGIN;
CREATE DATABASE kali_db OWNER kali_migrator;
'@
    Write-Output 'Criando somente as duas roles e o banco aprovados...'
    $createOutput = Invoke-Psql 'postgres' $createSql $adminPassword
    Write-Output $createOutput

    $passwordInput = "\password kali_migrator`n$migrationPassword`n$migrationPassword`n\password kali_app`n$appPassword`n$appPassword`n"
    Write-Output 'Definindo senhas via psql \password (não enviadas como SQL)...'
    $null = Invoke-Psql 'postgres' $passwordInput $adminPassword

    $grantsSql = @'
REVOKE ALL ON DATABASE kali_db FROM PUBLIC;
GRANT CONNECT ON DATABASE kali_db TO kali_migrator, kali_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO kali_app;
ALTER DEFAULT PRIVILEGES FOR ROLE kali_migrator IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO kali_app;
ALTER DEFAULT PRIVILEGES FOR ROLE kali_migrator IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO kali_app;
'@
    Write-Output 'Aplicando permissões mínimas somente em kali_db...'
    $grantsOutput = Invoke-Psql 'kali_db' $grantsSql $adminPassword
    Write-Output $grantsOutput

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

    $acl = Get-Acl $envFile
    $acl.SetAccessRuleProtection($true, $false)
    $currentSid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $accessRule = New-Object System.Security.AccessControl.FileSystemAccessRule `
        -ArgumentList $currentSid, 'FullControl', 'Allow'
    $acl.SetAccessRule($accessRule)
    Set-Acl -Path $envFile -AclObject $acl

    Write-Output 'Provisionamento concluído; .env criado com ACL restrita ao usuário atual. Nenhuma senha foi exibida.'
} finally {
    $adminPassword = $null
    $appPassword = $null
    $migrationPassword = $null
    $adminSecure = $null
    $appSecure = $null
    $migrationSecure = $null
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
