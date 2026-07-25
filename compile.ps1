$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$javac = Join-Path $root "target\jdk21\bin\javac.exe"
$classes = Join-Path $root "target\classes"
$libDir = Join-Path $root "target\lib"

New-Item -ItemType Directory -Force -Path $classes | Out-Null

$sources = Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
$libs = Get-ChildItem -Path $libDir -Filter "*.jar" | Select-Object -ExpandProperty FullName
$cp = ($libs -join ";") + ";" + $classes

Write-Host "Compiling $($sources.Count) Java source files with portable JDK 21..." -ForegroundColor Cyan
& $javac -encoding UTF-8 -d $classes -cp $cp $sources

# Copy resources from src/main/resources
$resDir = Join-Path $root "src\main\resources"
if (Test-Path $resDir) {
    Copy-Item -Path "$resDir\*" -Destination $classes -Recurse -Force
}

Write-Host "Compilation complete!" -ForegroundColor Green
