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

if ($config['DB_NAME'] -ne 'kali_db' -or $config['DB_USER'] -ne 'kali_app') {
    throw 'O .env não aponta para o banco/role runtime esperados.'
}

$userId = [guid]::NewGuid().ToString()
$consentId = [guid]::NewGuid().ToString()
$sessionPrimaryId = [guid]::NewGuid().ToString()
$sessionId = [guid]::NewGuid().ToString()
$email = "runtime-smoke-$userId@example.invalid"
$sql = @"
BEGIN;
INSERT INTO users (id, email, password_hash)
VALUES ('$userId', '$email', 'synthetic-rollback-only');
INSERT INTO user_profiles (user_id, display_name)
VALUES ('$userId', 'Synthetic rollback test');
UPDATE user_profiles SET display_name = 'Synthetic updated' WHERE user_id = '$userId';
INSERT INTO privacy_consents (id, user_id, purpose, policy_version, notice_text, granted_at)
VALUES ('$consentId', '$userId', 'TEST_ONLY', 'test', 'Synthetic rollback test', CURRENT_TIMESTAMP);
INSERT INTO period_entries (id, user_id, entry_date, bleeding_type, flow_level, cycle_start)
VALUES ('$([guid]::NewGuid())', '$userId', CURRENT_DATE - 1, 'PERIOD', 'LIGHT', TRUE);
UPDATE period_entries SET flow_level = 'MEDIUM' WHERE user_id = '$userId';
DELETE FROM period_entries WHERE user_id = '$userId';
INSERT INTO symptom_entries (id, user_id, entry_date, symptom_type, intensity, notes)
VALUES ('$([guid]::NewGuid())', '$userId', CURRENT_DATE - 1, 'CRAMPS', 3, 'Synthetic rollback test');
UPDATE symptom_entries SET intensity = 4 WHERE user_id = '$userId';
DELETE FROM symptom_entries WHERE user_id = '$userId';
INSERT INTO wellbeing_entries
    (id, user_id, entry_date, mood, stress_level, energy_level, sleep_quality, reflection)
VALUES
    ('$([guid]::NewGuid())', '$userId', CURRENT_DATE - 1, 'NEUTRAL', 2, 4, 3, 'Synthetic rollback test');
UPDATE wellbeing_entries SET mood = 'GOOD' WHERE user_id = '$userId';
DELETE FROM wellbeing_entries WHERE user_id = '$userId';
DELETE FROM privacy_consents WHERE id = '$consentId';
DELETE FROM user_profiles WHERE user_id = '$userId';
DELETE FROM users WHERE id = '$userId';
INSERT INTO spring_session
    (primary_id, session_id, creation_time, last_access_time, max_inactive_interval, expiry_time, principal_name)
VALUES
    ('$sessionPrimaryId', '$sessionId', 1, 1, 60, 61000, 'synthetic-smoke');
INSERT INTO spring_session_attributes (session_primary_id, attribute_name, attribute_bytes)
VALUES ('$sessionPrimaryId', 'synthetic', decode('73796e746865746963', 'hex'));
UPDATE spring_session SET last_access_time = 2 WHERE primary_id = '$sessionPrimaryId';
DELETE FROM spring_session WHERE primary_id = '$sessionPrimaryId';
ROLLBACK;
SELECT 'runtime-dml-smoke-rolled-back';
"@

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = $psql
$startInfo.Arguments = "-X -q -h $($config['DB_HOST']) -p $($config['DB_PORT']) -U kali_app -d kali_db -v ON_ERROR_STOP=1 -At"
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.EnvironmentVariables['PGPASSWORD'] = $config['DB_PASSWORD']

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
        throw "Runtime DML smoke test falhou (código $($process.ExitCode)): $($stderr.Trim())"
    }
    if ($stdout.Trim() -notmatch '\Aruntime-dml-smoke-rolled-back\z') {
        throw 'O runtime DML smoke test não retornou o marcador esperado.'
    }
    Write-Output 'Runtime DML smoke test aprovado; todos os dados sintéticos foram revertidos por ROLLBACK.'
} finally {
    $startInfo.EnvironmentVariables.Remove('PGPASSWORD')
    $config.Clear()
    $sql = $null
}
