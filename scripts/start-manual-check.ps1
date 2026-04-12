param(
    [switch]$Headless
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

function Stop-ListeningProcess {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object {
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        }
}

function Wait-Port {
    param(
        [Parameter(Mandatory = $true)]
        [string]$TargetHost,

        [Parameter(Mandatory = $true)]
        [int]$Port,

        [int]$Attempts = 30,

        [int]$DelaySeconds = 1
    )

    for ($i = 0; $i -lt $Attempts; $i++) {
        try {
            $tcpClient = New-Object Net.Sockets.TcpClient
            $tcpClient.Connect($TargetHost, $Port)
            $tcpClient.Close()
            return
        } catch {
            Start-Sleep -Seconds $DelaySeconds
        }
    }

    throw "Server did not start listening on $TargetHost`:$Port in time."
}

function New-Utf8ShellCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot,

        [Parameter(Mandatory = $true)]
        [string]$Command
    )

    $escapedProjectRoot = $ProjectRoot.Replace("'", "''")

    return @"
chcp 65001 > `$null
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
`$OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location '$escapedProjectRoot'
$Command
"@
}

function Invoke-Utf8GradleClient {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot
    )

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = "cmd.exe"
    $psi.Arguments = "/c chcp 65001>nul && .\\gradlew.bat runClient --no-daemon"
    $psi.WorkingDirectory = $ProjectRoot
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $psi.StandardErrorEncoding = [System.Text.Encoding]::UTF8

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $psi

    try {
        $process.Start() | Out-Null
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        $process.WaitForExit()

        if ($stdout) {
            [Console]::Out.Write($stdout)
        }

        if ($stderr) {
            [Console]::Out.Write($stderr)
        }

        return $process.ExitCode
    } finally {
        $process.Dispose()
    }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$serverHost = "127.0.0.1"
$serverPort = 3000
$serverEndpoint = "http://$serverHost`:$serverPort/mcp"

Write-Output "Building project..."
& .\gradlew.bat build
if ($LASTEXITCODE -ne 0) {
    throw "Build failed with exit code $LASTEXITCODE."
}

Stop-ListeningProcess -Port $serverPort

if ($Headless) {
    $tmpDir = Join-Path $projectRoot "build\tmp\manual-check"
    New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

    $serverStdout = Join-Path $tmpDir "server.out"
    $serverStderr = Join-Path $tmpDir "server.err"

    @($serverStdout, $serverStderr) | ForEach-Object {
        if (Test-Path $_) {
            Remove-Item $_ -Force
        }
    }

    $serverProcess = $null

    try {
        $serverProcess = Start-Process `
            -FilePath ".\gradlew.bat" `
            -ArgumentList "runServer", "--no-daemon" `
            -WorkingDirectory $projectRoot `
            -RedirectStandardOutput $serverStdout `
            -RedirectStandardError $serverStderr `
            -PassThru

        Wait-Port -TargetHost $serverHost -Port $serverPort

        Write-Output "Server is ready at $serverEndpoint"
        Write-Output "Running client in current console..."

        $clientExitCode = Invoke-Utf8GradleClient -ProjectRoot $projectRoot
        if ($clientExitCode -ne 0) {
            throw "Client failed with exit code $clientExitCode."
        }
    } finally {
        if ($serverProcess -and -not $serverProcess.HasExited) {
            Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
        }

        Stop-ListeningProcess -Port $serverPort
    }

    return
}

$serverCommand = New-Utf8ShellCommand `
    -ProjectRoot $projectRoot `
    -Command ".\gradlew.bat runServer"

$clientCommand = New-Utf8ShellCommand `
    -ProjectRoot $projectRoot `
    -Command ".\gradlew.bat runClient"

Write-Output "Opening server window..."
$serverWindow = Start-Process powershell `
    -ArgumentList "-NoExit", "-Command", $serverCommand `
    -WorkingDirectory $projectRoot `
    -PassThru

Wait-Port -TargetHost $serverHost -Port $serverPort

Write-Output "Opening client window..."
$clientWindow = Start-Process powershell `
    -ArgumentList "-NoExit", "-Command", $clientCommand `
    -WorkingDirectory $projectRoot `
    -PassThru

Write-Output "Manual check environment is ready."
Write-Output "Server window PID: $($serverWindow.Id)"
Write-Output "Client window PID: $($clientWindow.Id)"
Write-Output "Endpoint: $serverEndpoint"
