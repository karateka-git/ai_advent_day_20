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

function Assert-OutputContains {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text,

        [Parameter(Mandatory = $true)]
        [string[]]$ExpectedFragments
    )

    foreach ($fragment in $ExpectedFragments) {
        if (-not $Text.Contains($fragment)) {
            throw "Expected fragment was not found in client output: $fragment"
        }
    }
}

function Invoke-Utf8GradleClient {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectRoot,

        [Parameter(Mandatory = $true)]
        [string]$StdoutPath,

        [Parameter(Mandatory = $true)]
        [string]$StderrPath
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
        $exitCode = $process.ExitCode
    } finally {
        $process.Dispose()
    }

    [System.IO.File]::WriteAllText($StdoutPath, $stdout, [System.Text.Encoding]::UTF8)
    [System.IO.File]::WriteAllText($StderrPath, $stderr, [System.Text.Encoding]::UTF8)

    if ($stdout) {
        [Console]::Out.Write($stdout)
    }

    if ($stderr) {
        [Console]::Out.Write($stderr)
    }

    return $exitCode
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$serverHost = "127.0.0.1"
$serverPort = 3000
$serverEndpoint = "http://$serverHost`:$serverPort/mcp"

$tmpDir = Join-Path $projectRoot "build\tmp\e2e"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$serverStdout = Join-Path $tmpDir "server.out"
$serverStderr = Join-Path $tmpDir "server.err"
$clientStdout = Join-Path $tmpDir "client.out"
$clientStderr = Join-Path $tmpDir "client.err"

@($serverStdout, $serverStderr, $clientStdout, $clientStderr) | ForEach-Object {
    if (Test-Path $_) {
        Remove-Item $_ -Force
    }
}

$serverProcess = $null

try {
    Stop-ListeningProcess -Port $serverPort

    $serverProcess = Start-Process `
        -FilePath ".\gradlew.bat" `
        -ArgumentList "runServer", "--no-daemon" `
        -WorkingDirectory $projectRoot `
        -RedirectStandardOutput $serverStdout `
        -RedirectStandardError $serverStderr `
        -PassThru

    Wait-Port -TargetHost $serverHost -Port $serverPort

    $clientExitCode = Invoke-Utf8GradleClient `
        -ProjectRoot $projectRoot `
        -StdoutPath $clientStdout `
        -StderrPath $clientStderr

    if ($clientExitCode -ne 0) {
        $clientError = if (Test-Path $clientStderr) { Get-Content $clientStderr -Raw } else { "" }
        $clientOutput = if (Test-Path $clientStdout) { Get-Content $clientStdout -Raw } else { "" }
        throw "Client process failed with exit code $clientExitCode.`n$clientOutput`n$clientError"
    }

    $clientOutput = Get-Content $clientStdout -Raw
    Assert-OutputContains -Text $clientOutput -ExpectedFragments @(
        "Connected to MCP server: $serverEndpoint",
        "Server name: local_mcp_server",
        "Available tools (2):",
        "Ping [ping]",
        "Echo [echo]"
    )

    Write-Output "E2E check passed."
    Write-Output ""
    Write-Output "Client output:"
    Write-Output $clientOutput
} finally {
    if ($serverProcess -and -not $serverProcess.HasExited) {
        Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
    }

    Stop-ListeningProcess -Port $serverPort
}
