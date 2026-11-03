$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
$psql = 'C:\Program Files\PostgreSQL\17\bin\psql.exe'

if (-not (Test-Path $envFile)) {
    throw 'Arquivo .env não encontrado. Execute configure-postgres-env.ps1 primeiro.'
}
if (-not (Test-Path $psql)) {
    throw "psql não encontrado em '$psql'."
}

$originalEnvironment = @{}
$config = @{}
try {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        $trimmed = $line.Trim()
        if ($trimmed -and -not $trimmed.StartsWith('#') -and $trimmed.Contains('=')) {
            $parts = $trimmed.Split('=', 2)
            $key = $parts[0].Trim()
            $value = $parts[1].Trim()
            $config[$key] = $value
            if ($key -in @('DB_URL', 'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD',
                    'DB_MIGRATION_USER', 'DB_MIGRATION_PASSWORD')) {
                if (-not $originalEnvironment.ContainsKey($key)) {
                    $originalEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
                }
                [Environment]::SetEnvironmentVariable($key, $value, 'Process')
            }
        }
    }

    foreach ($key in @('DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD',
            'DB_MIGRATION_USER', 'DB_MIGRATION_PASSWORD')) {
        if (-not $config.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($config[$key])) {
            throw "Configuração obrigatória ausente no .env: $key"
        }
    }

    function Invoke-RoleQuery([string]$Username, [string]$Password, [string]$Sql) {
        $startInfo = New-Object System.Diagnostics.ProcessStartInfo
        $startInfo.FileName = $psql
        $startInfo.Arguments = "-X -h $($config['DB_HOST']) -p $($config['DB_PORT']) -U $Username -d $($config['DB_NAME']) -v ON_ERROR_STOP=1 -At -F |"
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardInput = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true
        $startInfo.EnvironmentVariables['PGPASSWORD'] = $Password

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $startInfo
        $null = $process.Start()
        $process.StandardInput.WriteLine($Sql)
        $process.StandardInput.Close()
        $output = $process.StandardOutput.ReadToEnd()
        $errorOutput = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw "Conexão/consulta PostgreSQL falhou para '$Username' (código $($process.ExitCode)): $($errorOutput.Trim())"
        }
        return $output.Trim()
    }

    $runtimeIdentitySql = @'
SELECT current_user,
       current_database(),
       has_database_privilege(current_user, current_database(), 'CONNECT'),
       has_database_privilege(current_user, current_database(), 'CREATE'),
       has_schema_privilege(current_user, 'public', 'USAGE'),
       has_schema_privilege(current_user, 'public', 'CREATE'),
       pg_has_role(current_user, 'kali_migrator', 'MEMBER');
'@
    $runtime = Invoke-RoleQuery $config['DB_USER'] $config['DB_PASSWORD'] $runtimeIdentitySql
    $runtimeIdentity = $runtime.Split('|')
    if ($runtimeIdentity.Count -ne 7 -or
        $runtimeIdentity[0] -ne 'kali_app' -or
        $runtimeIdentity[1] -ne $config['DB_NAME'] -or
        $runtimeIdentity[2] -ne 't' -or
        $runtimeIdentity[3] -ne 'f' -or
        $runtimeIdentity[4] -ne 't' -or
        $runtimeIdentity[5] -ne 'f' -or
        $runtimeIdentity[6] -ne 'f') {
        throw "Permissões inesperadas para a conta runtime: $runtime"
    }

    $runtimeGrantSql = @'
SELECT table_name,
       has_table_privilege('kali_app', format('public.%I', table_name), 'SELECT'),
       has_table_privilege('kali_app', format('public.%I', table_name), 'INSERT'),
       has_table_privilege('kali_app', format('public.%I', table_name), 'UPDATE'),
       has_table_privilege('kali_app', format('public.%I', table_name), 'DELETE'),
       has_table_privilege('kali_app', format('public.%I', table_name), 'TRUNCATE')
FROM (VALUES ('users'), ('user_profiles'), ('privacy_consents'),
             ('spring_session'), ('spring_session_attributes'),
             ('period_entries'), ('symptom_entries'),
             ('wellbeing_entries')) AS expected(table_name)
ORDER BY table_name;
'@
    $runtimeGrants = Invoke-RoleQuery $config['DB_MIGRATION_USER'] $config['DB_MIGRATION_PASSWORD'] $runtimeGrantSql
    $runtimeGrantLines = @($runtimeGrants -split "`r?`n" | Where-Object { $_ })
    $grantProblems = @()
    foreach ($line in $runtimeGrantLines) {
        $columns = $line.Split('|')
        if ($columns.Count -ne 6 -or
            $columns[1] -ne 't' -or $columns[2] -ne 't' -or
            $columns[3] -ne 't' -or $columns[4] -ne 't' -or
            $columns[5] -ne 'f') {
            $grantProblems += $line
        }
    }
    Write-Output 'Runtime DML permissions by application table:'
    $runtimeGrantLines | ForEach-Object { Write-Output $_ }
    $runtimeHistoryGrantSql = @'
SELECT has_table_privilege('kali_app', 'public.flyway_schema_history', 'SELECT'),
       has_table_privilege('kali_app', 'public.flyway_schema_history', 'INSERT'),
       has_table_privilege('kali_app', 'public.flyway_schema_history', 'UPDATE'),
       has_table_privilege('kali_app', 'public.flyway_schema_history', 'DELETE');
'@
    $runtimeHistoryGrants = Invoke-RoleQuery $config['DB_MIGRATION_USER'] $config['DB_MIGRATION_PASSWORD'] $runtimeHistoryGrantSql
    if ($runtimeHistoryGrants.Trim() -ne 'f|f|f|f') {
        throw "A role runtime recebeu privilégios inesperados no histórico Flyway: $runtimeHistoryGrants"
    }

    $migrationSql = @'
SELECT current_user,
       current_database(),
       has_database_privilege(current_user, current_database(), 'CONNECT'),
       has_schema_privilege(current_user, 'public', 'CREATE'),
       has_table_privilege(current_user, 'public.users', 'SELECT'),
       has_table_privilege(current_user, 'public.flyway_schema_history', 'SELECT'),
       has_table_privilege(current_user, 'public.flyway_schema_history', 'INSERT'),
       has_table_privilege(current_user, 'public.flyway_schema_history', 'UPDATE'),
       has_table_privilege(current_user, 'public.flyway_schema_history', 'DELETE');
SELECT version, success FROM public.flyway_schema_history ORDER BY installed_rank;
'@
    $migration = Invoke-RoleQuery $config['DB_MIGRATION_USER'] $config['DB_MIGRATION_PASSWORD'] $migrationSql
    $migrationLines = @($migration -split "`r?`n" | Where-Object { $_ })
    if ($migrationLines.Count -lt 4) {
        throw 'Histórico Flyway ausente ou incompleto.'
    }
    $migrationIdentity = $migrationLines[0].Split('|')
    if ($migrationIdentity[0] -ne 'kali_migrator' -or
        $migrationIdentity[1] -ne $config['DB_NAME'] -or
        $migrationIdentity[2] -ne 't' -or
        $migrationIdentity[3] -ne 't' -or
        $migrationIdentity[4] -ne 't') {
        throw "Permissões inesperadas para a conta de migração: $($migrationLines[0])"
    }
    $migrationVersions = @($migrationLines | Select-Object -Skip 1)
    if (($migrationVersions -join ',') -ne '1|t,2|t,3|t,4|t,5|t,6|t') {
        throw 'Flyway não registrou V1, V2, V3, V4, V5 e V6 como concluídas.'
    }

    $ownershipSql = @'
SELECT pg_get_userbyid(database.datdba),
       pg_get_userbyid(schema.nspowner)
FROM pg_database database
CROSS JOIN pg_namespace schema
WHERE database.datname = current_database() AND schema.nspname = 'public';
'@
    $ownership = Invoke-RoleQuery $config['DB_MIGRATION_USER'] $config['DB_MIGRATION_PASSWORD'] $ownershipSql

    $defaultDmlSql = @'
SELECT count(DISTINCT acl.privilege_type)
FROM pg_default_acl defaults
JOIN pg_roles owner_role ON owner_role.oid = defaults.defaclrole
JOIN pg_namespace schema_name ON schema_name.oid = defaults.defaclnamespace
CROSS JOIN LATERAL aclexplode(defaults.defaclacl) acl
JOIN pg_roles grantee ON grantee.oid = acl.grantee
WHERE owner_role.rolname = 'kali_migrator'
  AND schema_name.nspname = 'public'
  AND defaults.defaclobjtype = 'r'
  AND grantee.rolname = 'kali_app'
  AND acl.privilege_type IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE');
'@
    $defaultDmlCount = Invoke-RoleQuery $config['DB_MIGRATION_USER'] $config['DB_MIGRATION_PASSWORD'] $defaultDmlSql
    if ($defaultDmlCount.Trim() -ne '4') {
        throw "Default DML privileges for future tables are incomplete: count=$defaultDmlCount"
    }

    $roleAttributesSql = @'
SELECT rolname, rolsuper, rolcreatedb, rolcreaterole, rolreplication, rolbypassrls
FROM pg_roles
WHERE rolname IN ('kali_app', 'kali_migrator')
ORDER BY rolname;
'@
    $roleAttributes = Invoke-RoleQuery $config['DB_MIGRATION_USER'] $config['DB_MIGRATION_PASSWORD'] $roleAttributesSql
    Write-Output 'Role flags (superuser/create-db/create-role/replication/bypass-RLS):'
    $roleAttributeLines = @($roleAttributes -split "`r?`n" | Where-Object { $_ })
    $roleAttributeProblems = @()
    foreach ($line in $roleAttributeLines) {
        $columns = $line.Split('|')
        if ($columns.Count -ne 6 -or
            $columns[0] -notin @('kali_app', 'kali_migrator') -or
            $columns[1..5] -contains 't') {
            $roleAttributeProblems += $line
        }
    }
    if ($roleAttributeLines.Count -ne 2 -or $roleAttributeProblems.Count -gt 0) {
        throw "Role attributes exceed the approved scope: $roleAttributes"
    }
    $roleAttributeLines | ForEach-Object { Write-Output $_ }
    Write-Output 'Runtime role: login confirmado; CONNECT/USAGE; sem CREATE ou membership em kali_migrator.'
    Write-Output 'Migration role: login confirmado; CONNECT/CREATE; versões Flyway V1–V6 registradas como sucesso.'
    Write-Output "Database/schema owner: $ownership"
    Write-Output 'Default DML grants for future migrator-owned tables: SELECT/INSERT/UPDATE/DELETE.'
    if ($grantProblems.Count -gt 0) {
        throw "Permissões DML mínimas ausentes/inesperadas: $($grantProblems -join '; ')"
    }
} finally {
    foreach ($entry in $originalEnvironment.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
    }
    $config.Clear()
}
