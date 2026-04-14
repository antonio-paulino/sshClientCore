param(
    [string]$HostName = "",
    [string]$Username = "",
    [int]$Port = 22,
    [ValidateSet("SSHJ", "JSCH")]
    [string]$Backend = "JSCH",
    [bool]$TrustOnFirstUse = $true,
    [string]$KeyPath = ""
)

$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    # Ask for password securely unless key auth is provided.
    $plainPassword = ""
    if ([string]::IsNullOrWhiteSpace($KeyPath)) {
        $securePassword = Read-Host "SSH password for $Username@$HostName" -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try {
            $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }

    $env:SSH_HOST = $HostName
    $env:SSH_USER = $Username
    $env:SSH_PORT = $Port.ToString()
    $env:SSH_BACKEND = $Backend
    $env:SSH_TOFU = $TrustOnFirstUse.ToString().ToLowerInvariant()

    if ([string]::IsNullOrWhiteSpace($KeyPath)) {
        $env:SSH_PASS = $plainPassword
        Remove-Item Env:SSH_KEY -ErrorAction SilentlyContinue
    } else {
        $env:SSH_KEY = $KeyPath
        Remove-Item Env:SSH_PASS -ErrorAction SilentlyContinue
    }

    Write-Host "Starting sshClientCore terminal..." -ForegroundColor Cyan
    Write-Host "Host=$HostName Port=$Port User=$Username Backend=$Backend TOFU=$TrustOnFirstUse" -ForegroundColor DarkCyan

    # Build classes and write runtime classpath, then launch Java directly for real TTY support.
    & ".\gradlew.bat" writeRuntimeClasspath --no-daemon --console=plain

    $classpathFile = Join-Path $PSScriptRoot "build\runtime-classpath.txt"
    if (-not (Test-Path $classpathFile)) {
        throw "runtime-classpath.txt was not generated."
    }
    $runtimeClasspath = Get-Content $classpathFile -Raw
    if ([string]::IsNullOrWhiteSpace($runtimeClasspath)) {
        throw "runtime classpath is empty."
    }

    & "java" "-cp" $runtimeClasspath "pt.paulinoo.sshClientCore.MainKt"
}
finally {
    Pop-Location
}

