Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

function Stop-ListeningProcess {
    param([Parameter(Mandatory = $true)][int]$Port)

    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object {
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        }
}

function Wait-Port {
    param(
        [Parameter(Mandatory = $true)][string]$TargetHost,
        [Parameter(Mandatory = $true)][int]$Port,
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
        throw "$Description not found: $Path. Run .\gradlew.bat installClientDist, .\gradlew.bat installServerDist and .\gradlew.bat installStatefulServerDist first."
    }
}

function Assert-OutputContains {
    param(
        [Parameter(Mandatory = $true)][string]$Text,
        [Parameter(Mandatory = $true)][string[]]$ExpectedFragments
    )

    foreach ($fragment in $ExpectedFragments) {
        if (-not $Text.Contains($fragment)) {
            throw "Expected fragment was not found in client output: $fragment"
        }
    }
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

function Invoke-ClientCommand {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$BatPath,
        [Parameter(Mandatory = $true)][string[]]$CommandArgs,
        [Parameter(Mandatory = $true)][string]$StdoutPath,
        [Parameter(Mandatory = $true)][string]$StderrPath
    )

    $quotedArgs = $CommandArgs | ForEach-Object { "`"$_`"" }
    $command = "chcp 65001>nul && `"$BatPath`" $($quotedArgs -join ' ')"
    $process = Start-Process `
        -FilePath "cmd.exe" `
        -ArgumentList "/c", $command `
        -WorkingDirectory $ProjectRoot `
        -RedirectStandardOutput $StdoutPath `
        -RedirectStandardError $StderrPath `
        -PassThru `
        -Wait
    $exitCode = $process.ExitCode
    $process.Dispose()
    return $exitCode
}

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$serverHost = "127.0.0.1"
$statelessServerPort = 3000
$statefulServerPort = 3001
$statelessEndpoint = "http://$serverHost`:$statelessServerPort/mcp"
$statefulEndpoint = "http://$serverHost`:$statefulServerPort/mcp"

$tmpDir = Join-Path $projectRoot "build\tmp\e2e"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$summaryStoragePath = Join-Path $projectRoot "build\tmp\mcp-summary-storage\saved-summaries.json"
if (Test-Path -LiteralPath $summaryStoragePath) {
    Remove-Item -LiteralPath $summaryStoragePath -Force
}

$serverStdout = Join-Path $tmpDir "server.out"
$serverStderr = Join-Path $tmpDir "server.err"
$statefulServerStdout = Join-Path $tmpDir "stateful-server.out"
$statefulServerStderr = Join-Path $tmpDir "stateful-server.err"
$summaryClientStdout = Join-Path $tmpDir "client-summary.out"
$summaryClientStderr = Join-Path $tmpDir "client-summary.err"
$listClientStdout = Join-Path $tmpDir "client-list.out"
$listClientStderr = Join-Path $tmpDir "client-list.err"

$clientBatPath = Get-ClientBatPath -ProjectRoot $projectRoot
$serverBatPath = Get-ServerBatPath -ProjectRoot $projectRoot
$statefulServerBatPath = Get-StatefulServerBatPath -ProjectRoot $projectRoot

Write-Output "Building latest launcher distributions..."
& .\gradlew.bat installClientDist installServerDist installStatefulServerDist
if ($LASTEXITCODE -ne 0) {
    throw "Failed to build direct launchers. Gradle exit code: $LASTEXITCODE."
}

Assert-LauncherExists -Path $clientBatPath -Description "Built client launcher"
Assert-LauncherExists -Path $serverBatPath -Description "Built stateless server launcher"
Assert-LauncherExists -Path $statefulServerBatPath -Description "Built stateful server launcher"

@($serverStdout, $serverStderr, $statefulServerStdout, $statefulServerStderr, $summaryClientStdout, $summaryClientStderr, $listClientStdout, $listClientStderr) | ForEach-Object {
    if (Test-Path $_) {
        Remove-Item $_ -Force
    }
}

$serverProcess = $null
$statefulServerProcess = $null

try {
    Stop-ListeningProcess -Port $statelessServerPort
    Stop-ListeningProcess -Port $statefulServerPort

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

    $clientExitCode = Invoke-ClientCommand `
        -ProjectRoot $projectRoot `
        -BatPath $clientBatPath `
        -CommandArgs @("tool", "summary", "posts", "10", "long") `
        -StdoutPath $summaryClientStdout `
        -StderrPath $summaryClientStderr

    if ($clientExitCode -ne 0) {
        $clientError = if (Test-Path $summaryClientStderr) { Get-Content $summaryClientStderr -Raw } else { "" }
        $clientOutput = if (Test-Path $summaryClientStdout) { Get-Content $summaryClientStdout -Raw } else { "" }
        throw "Summary client process failed with exit code $clientExitCode.`n$clientOutput`n$clientError"
    }

    $listExitCode = Invoke-ClientCommand `
        -ProjectRoot $projectRoot `
        -BatPath $clientBatPath `
        -CommandArgs @("tool", "summaries") `
        -StdoutPath $listClientStdout `
        -StderrPath $listClientStderr

    if ($listExitCode -ne 0) {
        $clientError = if (Test-Path $listClientStderr) { Get-Content $listClientStderr -Raw } else { "" }
        $clientOutput = if (Test-Path $listClientStdout) { Get-Content $listClientStdout -Raw } else { "" }
        throw "List client process failed with exit code $listExitCode.`n$clientOutput`n$clientError"
    }

    $summaryOutput = Get-Content $summaryClientStdout -Raw
    $listOutput = Get-Content $listClientStdout -Raw
    Assert-OutputContains -Text $summaryOutput -ExpectedFragments @(
        "Summary pipeline выполнен успешно.",
        "Сохранён summary:",
        "Выбраны публикации:"
    )
    Assert-OutputContains -Text $listOutput -ExpectedFragments @(
        "Сохранённые summary:",
        "Summary по публикациям"
    )

    if (-not (Test-Path -LiteralPath $summaryStoragePath)) {
        throw "Expected summary storage file was not created: $summaryStoragePath"
    }

    Write-Output "E2E check passed."
    Write-Output ""
    Write-Output "Stateless endpoint: $statelessEndpoint"
    Write-Output "Stateful endpoint: $statefulEndpoint"
    Write-Output ""
    Write-Output "Summary command output:"
    Write-Output $summaryOutput
    Write-Output ""
    Write-Output "List command output:"
    Write-Output $listOutput
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
