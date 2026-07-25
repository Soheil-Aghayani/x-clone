# launch.ps1 — launches X Clone client connected to ArvanCloud online server or local backend
[CmdletBinding()]
param (
    [string]$ServerUrl = ""
)

$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

# Read server-url.txt if available
$serverFile = Join-Path $PSScriptRoot "server-url.txt"
if (-not $ServerUrl -and (Test-Path $serverFile)) {
    $ServerUrl = (Get-Content $serverFile -Raw).Trim()
}
if (-not $ServerUrl -and $env:XCLONE_SERVER_URL) {
    $ServerUrl = $env:XCLONE_SERVER_URL
}
if (-not $ServerUrl) {
    $ServerUrl = "https://x-clone.alirezalotfimoghaddam.ir"
}

# Dynamic discovery for Java
function Get-JavaPath {
    $localJdk = Join-Path $PSScriptRoot "target\jdk21\bin\java.exe"
    if (Test-Path $localJdk) { return $localJdk }
    $cmd = Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Path -First 1
    if ($cmd -and (Test-Path $cmd)) { return $cmd }
    if ($env:JAVA_HOME) {
        $jHome = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $jHome) { return $jHome }
    }
    $found = Get-ChildItem -Path "C:\Program Files", "C:\Program Files (x86)", "$env:LOCALAPPDATA\Programs" -Filter "java.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName -First 1
    if ($found) { return $found }
    return "java"
}

# Dynamic discovery for Maven
function Get-MvnPath {
    $cmd = Get-Command mvn -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Path -First 1
    if ($cmd -and (Test-Path $cmd)) { return $cmd }
    $found = Get-ChildItem -Path "C:\Program Files", "C:\Program Files (x86)", "$env:LOCALAPPDATA\Programs" -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName -First 1
    if ($found) { return $found }
    return "mvn"
}

$Java = Get-JavaPath
$Mvn  = Get-MvnPath

$M2      = Join-Path $env:USERPROFILE ".m2\repository"
$Classes = Join-Path $PSScriptRoot "target\classes"
$LibDir  = Join-Path $PSScriptRoot "target\lib"
$FxVer   = "21.0.6"

function Write-Step($n, $text) { Write-Host ""; Write-Host "[$n/3] $text" -ForegroundColor Cyan }
function Fail($msg) {
    Write-Host ""
    Write-Host "ERROR: $msg" -ForegroundColor Red
    Write-Host ""
    Write-Host "Press Enter to close..."
    [void][Console]::ReadLine()
    exit 1
}

function Find-Jar($namePattern, $m2RelPathPattern) {
    $inLib = Get-ChildItem -Path $LibDir -Filter $namePattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($inLib) { return $inLib.FullName }
    if ($m2RelPathPattern) {
        $fullPattern = Join-Path $M2 $m2RelPathPattern
        $found = Get-ChildItem -Path $fullPattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

Write-Host ""
Write-Host " ========================================" -ForegroundColor White
Write-Host "  X Clone Launcher" -ForegroundColor White
Write-Host " ========================================" -ForegroundColor White

# ── 1/3 Build & Copy Dependencies ─────────────────────────────────────────
Write-Step "1" "Preparing project dependencies..."
try {
    & $Mvn -f .\pom.xml compile dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib -q
} catch {
    Write-Host "      Maven compile skipped..." -ForegroundColor Yellow
}

if (-not (Test-Path -LiteralPath (Join-Path $Classes "client\launcher.class"))) {
    $compileScript = Join-Path $PSScriptRoot "compile.ps1"
    if (Test-Path $compileScript) {
        & powershell -ExecutionPolicy Bypass -File $compileScript
    }
}

if (-not (Test-Path -LiteralPath (Join-Path $Classes "client\launcher.class"))) {
    Fail "client launcher.class missing in target\classes. Please run 'compile.ps1' or 'mvn compile'."
}
Write-Host "      Build OK." -ForegroundColor Green

# ── Classpath Construction ────────────────────────────────────────────────
$allTargetJars = Get-ChildItem -Path $LibDir -Filter "*.jar" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
if ($allTargetJars) {
    $ClientCp = "$Classes;" + ($allTargetJars -join ";")
} else {
    $Gson       = Find-Jar "gson-*.jar" "com\google\code\gson\gson\*\gson-*.jar"
    $Bcrypt     = Find-Jar "bcrypt-*.jar" "at\favre\lib\bcrypt\*\bcrypt-*.jar"
    $Bytes      = Find-Jar "bytes-*.jar" "at\favre\lib\bytes\*\bytes-*.jar"
    $Sqlite     = Find-Jar "sqlite-jdbc-*.jar" "org\xerial\sqlite-jdbc\*\sqlite-jdbc-*.jar"
    $ClientCp   = "$Classes;$Gson;$Bcrypt;$Bytes;$Sqlite"
}

$FxControls = Find-Jar "javafx-controls-*-win.jar" "org\openjfx\javafx-controls\$FxVer\javafx-controls-$FxVer-win.jar"
if (-not $FxControls) { $FxControls = Find-Jar "javafx-controls-*.jar" "org\openjfx\javafx-controls\$FxVer\javafx-controls-$FxVer.jar" }

$FxGraphics = Find-Jar "javafx-graphics-*-win.jar" "org\openjfx\javafx-graphics\$FxVer\javafx-graphics-$FxVer-win.jar"
if (-not $FxGraphics) { $FxGraphics = Find-Jar "javafx-graphics-*.jar" "org\openjfx\javafx-graphics\$FxVer\javafx-graphics-$FxVer.jar" }

$FxBase     = Find-Jar "javafx-base-*-win.jar" "org\openjfx\javafx-base\$FxVer\javafx-base-$FxVer-win.jar"
if (-not $FxBase) { $FxBase = Find-Jar "javafx-base-*.jar" "org\openjfx\javafx-base\$FxVer\javafx-base-$FxVer.jar" }

$FxFxml     = Find-Jar "javafx-fxml-*-win.jar" "org\openjfx\javafx-fxml\$FxVer\javafx-fxml-$FxVer-win.jar"
if (-not $FxFxml) { $FxFxml = Find-Jar "javafx-fxml-*.jar" "org\openjfx\javafx-fxml\$FxVer\javafx-fxml-$FxVer.jar" }

$ModulePath = "$FxControls;$FxGraphics;$FxBase;$FxFxml"

# ── 2/3 Server Connection Setup ───────────────────────────────────────────
$isLocal = ($ServerUrl -like "*127.0.0.1*" -or $ServerUrl -like "*localhost*")
$serverProcess = $null

if ($isLocal) {
    Write-Step "2" "Starting local backend on port 8080..."

    Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "      Stopping old process on 8080 (PID $($_.OwningProcess))...."
        Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Milliseconds 500

    $serverOut = Join-Path $env:TEMP "xclone-server-out.log"
    $serverErr = Join-Path $env:TEMP "xclone-server-err.log"
    Remove-Item $serverOut, $serverErr -ErrorAction SilentlyContinue

    $serverProcess = Start-Process -FilePath $Java `
        -ArgumentList @("-cp", $ClientCp, "server.network.server") `
        -WorkingDirectory $PSScriptRoot `
        -WindowStyle Minimized `
        -RedirectStandardOutput $serverOut `
        -RedirectStandardError $serverErr `
        -PassThru

    $up = $false
    for ($i = 0; $i -lt 25; $i++) {
        Start-Sleep -Milliseconds 400
        if ($serverProcess.HasExited) { break }
        if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
            $up = $true
            break
        }
    }

    if (-not $up) {
        $errText = ""
        if (Test-Path $serverErr) { $errText = Get-Content $serverErr -Raw }
        if (Test-Path $serverOut) { $errText += Get-Content $serverOut -Raw }
        if ($serverProcess -and -not $serverProcess.HasExited) { Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue }
        Fail "Backend did not open port 8080.`n$errText"
    }

    Write-Host "      Local server is up (PID $($serverProcess.Id))." -ForegroundColor Green
} else {
    Write-Step "2" "Connecting to online server: $ServerUrl"
}

# ── 3/3 Client ────────────────────────────────────────────────────────────
Write-Step "3" "Opening X Clone window..."
Write-Host ""
Write-Host " Connected to: $ServerUrl" -ForegroundColor Cyan
Write-Host " Keep this window open while using the app." -ForegroundColor Yellow
Write-Host ""

$clientExit = 0
try {
    $client = Start-Process -FilePath $Java `
        -ArgumentList @(
            "--module-path", $ModulePath,
            "--add-modules", "javafx.controls,javafx.fxml",
            "-Dprism.order=d3d,sw",
            "-Dxclone.server.url=$ServerUrl",
            "-cp", $ClientCp,
            "client.launcher"
        ) `
        -WorkingDirectory $PSScriptRoot `
        -Wait `
        -PassThru
    $clientExit = $client.ExitCode
} catch {
    Write-Host "Client failed to start: $_" -ForegroundColor Red
    $clientExit = 1
}

# ── Shutdown ──────────────────────────────────────────────────────────────
if ($serverProcess -and -not $serverProcess.HasExited) {
    Write-Host ""
    Write-Host " Shutting down local server..."
    Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
}

if ($clientExit -ne 0) {
    Fail "Client exited with code $clientExit."
}

Write-Host " Done." -ForegroundColor Green
exit 0
