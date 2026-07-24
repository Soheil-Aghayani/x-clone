# launch.ps1 — starts backend (port 8080) then the JavaFX client
$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot

$Mvn      = "C:\Users\Soheil\AppData\Local\Programs\apache-maven-3.9.16\bin\mvn.cmd"
$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$Java     = Join-Path $JavaHome "bin\java.exe"
$M2       = Join-Path $env:USERPROFILE ".m2\repository"
$Classes  = Join-Path $PSScriptRoot "target\classes"
$FxVer    = "21.0.6"

function Write-Step($n, $text) { Write-Host ""; Write-Host "[$n/3] $text" -ForegroundColor Cyan }
function Fail($msg) {
    Write-Host ""
    Write-Host "ERROR: $msg" -ForegroundColor Red
    Write-Host ""
    Write-Host "Press Enter to close..."
    [void][Console]::ReadLine()
    exit 1
}

Write-Host ""
Write-Host " ========================================" -ForegroundColor White
Write-Host "  X Clone Launcher" -ForegroundColor White
Write-Host " ========================================" -ForegroundColor White

if (-not (Test-Path -LiteralPath $Java)) { Fail "Java not found at:`n  $Java`nUpdate `$JavaHome in launch.ps1" }
if (-not (Test-Path -LiteralPath $Mvn))  { Fail "Maven not found at:`n  $Mvn`nUpdate `$Mvn in launch.ps1" }

# ── 1/3 Build ─────────────────────────────────────────────────────────────
Write-Step "1" "Building project..."
$env:JAVA_HOME = $JavaHome
# IMPORTANT: do not pass Windows paths with backslashes to mvn.cmd from
# PowerShell — cmd eats "\p" etc. and turns "...\twitter-clone\pom.xml"
# into "...\twitter-clonepom.xml". Use a relative pom after Set-Location.
$pom = Join-Path $PSScriptRoot "pom.xml"
if (-not (Test-Path -LiteralPath $pom)) { Fail "pom.xml not found at:`n  $pom" }
& $Mvn -f .\pom.xml compile -q
if ($LASTEXITCODE -ne 0) { Fail "Maven compile failed. Scroll up for details." }
if (-not (Test-Path -LiteralPath (Join-Path $Classes "server\network\server.class"))) { Fail "server.class missing after compile." }
if (-not (Test-Path -LiteralPath (Join-Path $Classes "client\launcher.class")))      { Fail "client launcher.class missing after compile." }
Write-Host "      Build OK." -ForegroundColor Green

# ── jars ──────────────────────────────────────────────────────────────────
$Gson   = Join-Path $M2 "com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"
$Bcrypt = Join-Path $M2 "at\favre\lib\bcrypt\0.10.2\bcrypt-0.10.2.jar"
$Bytes  = Join-Path $M2 "at\favre\lib\bytes\1.5.0\bytes-1.5.0.jar"
$Sqlite = Join-Path $M2 "org\xerial\sqlite-jdbc\3.53.2.1\sqlite-jdbc-3.53.2.1.jar"
$FxControls = Join-Path $M2 "org\openjfx\javafx-controls\$FxVer\javafx-controls-$FxVer-win.jar"
$FxGraphics = Join-Path $M2 "org\openjfx\javafx-graphics\$FxVer\javafx-graphics-$FxVer-win.jar"
$FxBase     = Join-Path $M2 "org\openjfx\javafx-base\$FxVer\javafx-base-$FxVer-win.jar"
$FxFxml     = Join-Path $M2 "org\openjfx\javafx-fxml\$FxVer\javafx-fxml-$FxVer-win.jar"

foreach ($j in @($Gson, $Bcrypt, $Bytes, $Sqlite, $FxControls, $FxGraphics, $FxBase, $FxFxml)) {
    if (-not (Test-Path -LiteralPath $j)) {
        Fail "Missing jar:`n  $j`n`nRun once in this folder:`n  mvn dependency:resolve"
    }
}

$ServerCp   = "$Classes;$Gson;$Bcrypt;$Bytes;$Sqlite"
$ClientCp   = $ServerCp
$ModulePath = "$FxControls;$FxGraphics;$FxBase;$FxFxml"

# ── 2/3 Backend ───────────────────────────────────────────────────────────
Write-Step "2" "Starting backend on port 8080..."

# Free port 8080
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "      Stopping old process on 8080 (PID $($_.OwningProcess))..."
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Milliseconds 500

$serverOut = Join-Path $env:TEMP "xclone-server-out.log"
$serverErr = Join-Path $env:TEMP "xclone-server-err.log"
Remove-Item $serverOut, $serverErr -ErrorAction SilentlyContinue

$server = Start-Process -FilePath $Java `
    -ArgumentList @("-cp", $ServerCp, "server.network.server") `
    -WorkingDirectory $PSScriptRoot `
    -WindowStyle Minimized `
    -RedirectStandardOutput $serverOut `
    -RedirectStandardError $serverErr `
    -PassThru

$up = $false
for ($i = 0; $i -lt 25; $i++) {
    Start-Sleep -Milliseconds 400
    if ($server.HasExited) { break }
    if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
        $up = $true
        break
    }
}

if (-not $up) {
    $errText = ""
    if (Test-Path $serverErr) { $errText = Get-Content $serverErr -Raw }
    if (Test-Path $serverOut) { $errText += Get-Content $serverOut -Raw }
    if (-not $server.HasExited) { Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue }
    Fail "Backend did not open port 8080.`n$errText"
}

Write-Host "      Server is up (PID $($server.Id))." -ForegroundColor Green

# ── 3/3 Client ────────────────────────────────────────────────────────────
Write-Step "3" "Opening X Clone window..."
Write-Host ""
Write-Host " Keep this window open while you use the app." -ForegroundColor Yellow
Write-Host " Close the app window when you are done." -ForegroundColor Yellow
Write-Host ""

$clientExit = 0
try {
    $client = Start-Process -FilePath $Java `
        -ArgumentList @(
            "--module-path", $ModulePath,
            "--add-modules", "javafx.controls,javafx.fxml",
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
Write-Host ""
Write-Host " Shutting down server..."
if (-not $server.HasExited) {
    Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
}
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}

if ($clientExit -ne 0) {
    Fail "Client exited with code $clientExit."
}

Write-Host " Done." -ForegroundColor Green
Write-Host ""
Write-Host "Press Enter to close..."
[void][Console]::ReadLine()
exit 0
