param(
    [switch]$Headless,
    [switch]$SkipBuild
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

function Get-ClientBatPath {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)
    Join-Path $ProjectRoot "build\install\mcp-client\bin\mcp-client.bat"
}

function Get-ServerBatPath {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)
    Join-Path $ProjectRoot "build\install\mcp-server\bin\mcp-server.bat"
}

function Get-StatefulServerBatPath {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)
    Join-Path $ProjectRoot "build\install\mcp-stateful-server\bin\mcp-stateful-server.bat"
}

function Assert-LauncherExists {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Description not found: $Path. Run .\gradlew.bat build, .\gradlew.bat installClientDist, .\gradlew.bat installServerDist and .\gradlew.bat installStatefulServerDist first."
    }
}

function New-Utf8ShellCommand {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$BatPath
    )

    $escapedProjectRoot = $ProjectRoot.Replace("'", "''")
    $escapedBatPath = $BatPath.Replace("'", "''")

    return @"
chcp 65001 > `$null
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
`$OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location '$escapedProjectRoot'
& '$escapedBatPath'
"@
}

function Start-HeadlessLauncherProcess {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$BatPath,
        [Parameter(Mandatory = $true)][string]$StdoutPath,
        [Parameter(Mandatory = $true)][string]$StderrPath
    )

    $command = "chcp 65001>nul && `"$BatPath`""

    Start-Process `
        -FilePath "cmd.exe" `
        -ArgumentList "/c", $command `
        -WorkingDirectory $ProjectRoot `
        -RedirectStandardOutput $StdoutPath `
        -RedirectStandardError $StderrPath `
        -PassThru
}

function Stop-HeadlessLauncherProcess {
    param([System.Diagnostics.Process]$Process)

    if ($Process) {
        try {
            if (-not $Process.HasExited) {
                Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
            }
        } finally {
            $Process.Dispose()
        }
    }
}

function Invoke-Utf8ClientCommands {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$StdoutPath,
        [Parameter(Mandatory = $true)][string]$StderrPath
    )

    $null = & powershell -ExecutionPolicy Bypass -File (Join-Path $ProjectRoot "scripts\invoke-client-commands.ps1") `
        -ProjectRoot $ProjectRoot `
        -Commands @("help", "tool posts", "tool start-random-posts 1", "exit") `
        -StdoutPath $StdoutPath `
        -StderrPath $StderrPath

    if (Test-Path $StdoutPath) {
        [Console]::Out.Write((Get-Content $StdoutPath -Raw))
    }

    if (Test-Path $StderrPath) {
        [Console]::Out.Write((Get-Content $StderrPath -Raw))
    }

    return $LASTEXITCODE
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$serverHost = "127.0.0.1"
$statelessServerPort = 3000
$statefulServerPort = 3001
$statelessEndpoint = "http://$serverHost`:$statelessServerPort/mcp"
$statefulEndpoint = "http://$serverHost`:$statefulServerPort/mcp"

if ($SkipBuild) {
    Write-Output "Skipping build step and reusing existing artifacts..."
} else {
    Write-Output "Building project and direct launchers..."
    & .\gradlew.bat build installClientDist installServerDist installStatefulServerDist
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed with exit code $LASTEXITCODE."
    }
}

$clientBatPath = Get-ClientBatPath -ProjectRoot $projectRoot
$serverBatPath = Get-ServerBatPath -ProjectRoot $projectRoot
$statefulServerBatPath = Get-StatefulServerBatPath -ProjectRoot $projectRoot

Assert-LauncherExists -Path $clientBatPath -Description "Built client launcher"
Assert-LauncherExists -Path $serverBatPath -Description "Built stateless server launcher"
Assert-LauncherExists -Path $statefulServerBatPath -Description "Built stateful server launcher"

Stop-ListeningProcess -Port $statelessServerPort
Stop-ListeningProcess -Port $statefulServerPort

if ($Headless) {
    $tmpDir = Join-Path $projectRoot "build\tmp\manual-check"
    New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

    $serverStdout = Join-Path $tmpDir "server.out"
    $serverStderr = Join-Path $tmpDir "server.err"
    $statefulServerStdout = Join-Path $tmpDir "stateful-server.out"
    $statefulServerStderr = Join-Path $tmpDir "stateful-server.err"
    $clientStdout = Join-Path $tmpDir "client.out"
    $clientStderr = Join-Path $tmpDir "client.err"

    @($serverStdout, $serverStderr, $statefulServerStdout, $statefulServerStderr, $clientStdout, $clientStderr) | ForEach-Object {
        if (Test-Path $_) {
            try {
                Remove-Item $_ -Force
            } catch {
                Remove-Item $_ -Force -ErrorAction SilentlyContinue
            }
        }
    }

    $serverProcess = $null
    $statefulServerProcess = $null

    try {
        $serverProcess = Start-HeadlessLauncherProcess `
            -ProjectRoot $projectRoot `
            -BatPath $serverBatPath `
            -StdoutPath $serverStdout `
            -StderrPath $serverStderr

        $statefulServerProcess = Start-HeadlessLauncherProcess `
            -ProjectRoot $projectRoot `
            -BatPath $statefulServerBatPath `
            -StdoutPath $statefulServerStdout `
            -StderrPath $statefulServerStderr

        Wait-Port -TargetHost $serverHost -Port $statelessServerPort
        Wait-Port -TargetHost $serverHost -Port $statefulServerPort

        Write-Output "Stateless server is ready at $statelessEndpoint"
        Write-Output "Stateful server is ready at $statefulEndpoint"
        Write-Output "Running client in current console..."

        $clientExitCode = Invoke-Utf8ClientCommands `
            -ProjectRoot $projectRoot `
            -StdoutPath $clientStdout `
            -StderrPath $clientStderr

        if ($clientExitCode -ne 0) {
            throw "Client failed with exit code $clientExitCode."
        }
    } finally {
        if ($serverProcess) {
            Stop-HeadlessLauncherProcess -Process $serverProcess
        }
        if ($statefulServerProcess) {
            Stop-HeadlessLauncherProcess -Process $statefulServerProcess
        }

        Stop-ListeningProcess -Port $statelessServerPort
        Stop-ListeningProcess -Port $statefulServerPort
    }

    return
}

$serverCommand = New-Utf8ShellCommand -ProjectRoot $projectRoot -BatPath $serverBatPath
$statefulServerCommand = New-Utf8ShellCommand -ProjectRoot $projectRoot -BatPath $statefulServerBatPath
$clientCommand = New-Utf8ShellCommand -ProjectRoot $projectRoot -BatPath $clientBatPath

Write-Output "Opening stateless server window..."
$serverWindow = Start-Process powershell `
    -ArgumentList "-NoExit", "-Command", $serverCommand `
    -WorkingDirectory $projectRoot `
    -PassThru

Write-Output "Opening stateful server window..."
$statefulServerWindow = Start-Process powershell `
    -ArgumentList "-NoExit", "-Command", $statefulServerCommand `
    -WorkingDirectory $projectRoot `
    -PassThru

Wait-Port -TargetHost $serverHost -Port $statelessServerPort
Wait-Port -TargetHost $serverHost -Port $statefulServerPort

Write-Output "Opening client window..."
$clientWindow = Start-Process powershell `
    -ArgumentList "-NoExit", "-Command", $clientCommand `
    -WorkingDirectory $projectRoot `
    -PassThru

Write-Output "Manual check environment is ready."
Write-Output "Stateless server window PID: $($serverWindow.Id)"
Write-Output "Stateful server window PID: $($statefulServerWindow.Id)"
Write-Output "Client window PID: $($clientWindow.Id)"
Write-Output "Stateless endpoint: $statelessEndpoint"
Write-Output "Stateful endpoint: $statefulEndpoint"
