$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
$psql = 'C:\Program Files\PostgreSQL\17\bin\psql.exe'

if (-not (Test-Path $envFile)) {
    throw 'Arquivo .env não encontrado.'
}
if (-not (Test-Path $psql)) {
    throw "psql não encontrado em '$psql'."
}

$config = @{}
foreach ($line in Get-Content -LiteralPath $envFile) {
    $trimmed = $line.Trim()
    if ($trimmed -and -not $trimmed.StartsWith('#') -and $trimmed.Contains('=')) {
        $parts = $trimmed.Split('=', 2)
        $config[$parts[0].Trim()] = $parts[1].Trim()
    }
}

if ($config['DB_NAME'] -ne 'kali_db' -or
    $config['DB_USER'] -ne 'kali_app' -or
    $config['DB_MIGRATION_USER'] -ne 'kali_migrator') {
    throw 'O .env não identifica exatamente o banco e as duas roles aprovadas; nenhum comando foi executado.'
}

$sql = @'
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE
  public.users,
  public.user_profiles,
  public.privacy_consents,
  public.spring_session,
  public.spring_session_attributes,
  public.period_entries,
  public.symptom_entries,
  public.wellbeing_entries
TO kali_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO kali_app;
'@

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = $psql
$startInfo.Arguments = "-X -h $($config['DB_HOST']) -p $($config['DB_PORT']) -U kali_migrator -d kali_db -v ON_ERROR_STOP=1 -At"
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.EnvironmentVariables['PGPASSWORD'] = $config['DB_MIGRATION_PASSWORD']

try {
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    $null = $process.Start()
    $process.StandardInput.WriteLine($sql)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw "Aplicação dos grants falhou (código $($process.ExitCode)): $($stderr.Trim())"
    }

    Write-Output 'Grants DML aprovados aplicados em kali_db; nenhuma senha foi exibida.'
    if ($stdout.Trim()) {
        Write-Output $stdout.Trim()
    }
} finally {
    $startInfo.EnvironmentVariables.Remove('PGPASSWORD')
    $config.Clear()
    $sql = $null
}
