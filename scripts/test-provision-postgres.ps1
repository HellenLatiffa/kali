$ErrorActionPreference = 'Stop'
$helperModule = Join-Path $PSScriptRoot 'PostgresProvisioning.Helpers.psm1'
Import-Module $helperModule -Force

$tests = @(
    @{
        Name = 'accepts 24-character alphanumeric passwords'
        App = 'AlphaNumericPassword2026'
        Migration = 'MigrationPassword2026ABC'
        Expected = $true
    },
    @{
        Name = 'accepts common printable punctuation'
        App = 'App-Pass!word_2026+Secure'
        Migration = 'Migrator@Pass#2026-Strong'
        Expected = $true
    },
    @{
        Name = 'rejects identical passwords'
        App = 'SamePassword-2026-Strong!'
        Migration = 'SamePassword-2026-Strong!'
        Expected = $false
    },
    @{
        Name = 'accepts case-distinct passwords'
        App = 'CaseDistinctPassword2026A'
        Migration = 'casedistinctpassword2026a'
        Expected = $true
    },
    @{
        Name = 'rejects passwords shorter than 24 characters'
        App = 'Short-Password-2026'
        Migration = 'Another-Short-Password'
        Expected = $false
    },
    @{
        Name = 'rejects leading, trailing, and embedded spaces'
        App = ' AppPassword-2026-Strong!'
        Migration = 'Migration Password 2026!'
        Expected = $false
    },
    @{
        Name = 'rejects backslash for Java properties compatibility'
        App = 'ApplicationPassword-2026\'
        Migration = 'MigrationPassword-2026!!'
        Expected = $false
    },
    @{
        Name = 'rejects Spring placeholder syntax'
        App = 'ApplicationPassword-${foo}-2026'
        Migration = 'MigrationPassword-2026!'
        Expected = $false
    },
    @{
        Name = 'rejects property-file comment prefix'
        App = '#ApplicationPassword-2026-Strong'
        Migration = 'MigrationPassword-2026!'
        Expected = $false
    },
    @{
        Name = 'rejects alternate property-file comment prefix'
        App = '!ApplicationPassword-2026-Strong'
        Migration = 'MigrationPassword-2026!'
        Expected = $false
    },
    @{
        Name = 'rejects a trailing newline'
        App = "ApplicationPassword-2026!`n"
        Migration = 'MigrationPassword-2026!'
        Expected = $false
    },
    @{
        Name = 'rejects non-ASCII characters'
        App = 'ApplicationPassword-2026-é'
        Migration = 'MigrationPassword-2026!'
        Expected = $false
    }
)

$failures = 0
foreach ($test in $tests) {
    $actual = Test-KaliRolePassword $test.App $test.Migration
    if ($actual -ne $test.Expected) {
        Write-Output "FAIL: $($test.Name)"
        $failures++
    } else {
        Write-Output "PASS: $($test.Name)"
    }
}

$dummyPassword = 'DummyPassword-2026!Secure'
$secureValue = ConvertTo-SecureString $dummyPassword -AsPlainText -Force
$roundTrip = ConvertTo-KaliPlainText $secureValue
if (-not [string]::Equals($dummyPassword, $roundTrip, [StringComparison]::Ordinal)) {
    Write-Output 'FAIL: SecureString conversion preserves the synthetic input exactly'
    $failures++
} else {
    Write-Output 'PASS: SecureString conversion preserves the synthetic input exactly'
}

if ($failures -gt 0) {
    throw "$failures password validation test(s) failed."
}

Write-Output "All $($tests.Count + 1) password validation tests passed."
