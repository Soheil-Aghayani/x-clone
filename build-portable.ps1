param(
    [string]$ServerUrl = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$project = Split-Path -Parent $MyInvocation.MyCommand.Path
$target = Join-Path $project "target"
$input = Join-Path $target "portable-input"
$dist = Join-Path $project "dist"
$appName = "X Clone"
$appDir = Join-Path $dist $appName
$zip = Join-Path $dist "X-Clone-Windows-Portable.zip"

Push-Location $project
try {
    if (-not (Get-Command mvn.cmd -ErrorAction SilentlyContinue)) {
        throw "Maven is required to build the portable package."
    }
    if (-not (Get-Command jpackage.exe -ErrorAction SilentlyContinue)) {
        throw "JDK 21 with jpackage is required to build the portable package."
    }

    & mvn.cmd clean package dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$input"
    if ($LASTEXITCODE -ne 0) { throw "Maven package build failed." }

    New-Item -ItemType Directory -Force -Path $input | Out-Null
    Copy-Item -LiteralPath (Join-Path $target "TwitterClone-1.0-SNAPSHOT.jar") -Destination $input -Force
    Get-ChildItem -LiteralPath $input -Filter "javafx-*.jar" | Remove-Item -Force

    $javaFxModules = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\openjfx" -Recurse -Filter "*-win.jar" |
        Where-Object { $_.Name -match "^javafx-(base|graphics|controls|fxml)-" } |
        ForEach-Object FullName
    if (-not $javaFxModules) { throw "JavaFX Windows modules were not found in the Maven repository." }
    $modulePath = $javaFxModules -join ";"

    if (Test-Path -LiteralPath $appDir) {
        $resolvedDist = [System.IO.Path]::GetFullPath($dist)
        $resolvedApp = [System.IO.Path]::GetFullPath($appDir)
        if (-not $resolvedApp.StartsWith($resolvedDist, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove an app directory outside dist."
        }
        Remove-Item -LiteralPath $appDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $dist | Out-Null

    $arguments = @(
        "--type", "app-image",
        "--name", $appName,
        "--app-version", "1.0.0",
        "--vendor", "X Clone",
        "--description", "Local X-style social desktop application",
        "--dest", $dist,
        "--input", $input,
        "--main-jar", "TwitterClone-1.0-SNAPSHOT.jar",
        "--main-class", "client.launcher",
        "--module-path", $modulePath,
        "--add-modules", "javafx.controls,javafx.fxml,java.sql,java.naming,java.net.http,jdk.httpserver,jdk.crypto.ec",
        "--java-options", "-Dfile.encoding=UTF-8",
        "--icon", (Join-Path $project "src\main\resources\icons\app\x-clone.ico")
    )
    & jpackage.exe @arguments
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

    Copy-Item -LiteralPath (Join-Path $project "README-FIRST.txt") -Destination $appDir -Force
    if (-not [string]::IsNullOrWhiteSpace($ServerUrl)) {
        if (-not $ServerUrl.StartsWith("https://", [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "ServerUrl must begin with https:// for a shared build."
        }
        Set-Content -LiteralPath (Join-Path $appDir "server-url.txt") -Value $ServerUrl.Trim() -Encoding utf8NoBOM
    }
    if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip -Force }
    Compress-Archive -LiteralPath $appDir -DestinationPath $zip -CompressionLevel Optimal

    Write-Host ""
    Write-Host "Portable application: $appDir"
    Write-Host "Share this ZIP:       $zip"
} finally {
    Pop-Location
}
