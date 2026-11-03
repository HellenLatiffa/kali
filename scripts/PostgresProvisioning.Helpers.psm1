function ConvertTo-KaliPlainText {
    param(
        [Parameter(Mandatory = $true)]
        [Security.SecureString]$Value
    )

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Test-KaliRolePassword {
    param(
        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$ApplicationPassword,
        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$MigrationPassword
    )

    $passwordPattern = '\A[\x21-\x5B\x5D-\x7E]{24,}\z'
    $applicationMatches = [regex]::IsMatch($ApplicationPassword, $passwordPattern) -and
        -not $ApplicationPassword.Contains('${') -and
        $ApplicationPassword[0] -notin @('#', '!')
    $migrationMatches = [regex]::IsMatch($MigrationPassword, $passwordPattern) -and
        -not $MigrationPassword.Contains('${') -and
        $MigrationPassword[0] -notin @('#', '!')
    $samePassword = [string]::Equals(
        $ApplicationPassword,
        $MigrationPassword,
        [StringComparison]::Ordinal
    )

    return ($applicationMatches -and $migrationMatches -and -not $samePassword)
}

Export-ModuleMember -Function ConvertTo-KaliPlainText, Test-KaliRolePassword
