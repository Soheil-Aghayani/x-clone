$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$Java = "$JavaHome\bin\java.exe"
$Mvn = "C:\Users\Soheil\AppData\Local\Programs\apache-maven-3.9.16\bin\mvn.cmd"
$M2 = "$env:USERPROFILE\.m2\repository"
$Classes = "$PSScriptRoot\target\classes"
$FxVer = "21.0.6"

$env:JAVA_HOME = $JavaHome

Write-Host "Building latest X Clone client..." -ForegroundColor Cyan
& $Mvn -f "$PSScriptRoot\pom.xml" compile -q

$Gson = "$M2\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar"
$Bcrypt = "$M2\at\favre\lib\bcrypt\0.10.2\bcrypt-0.10.2.jar"
$Bytes = "$M2\at\favre\lib\bytes\1.5.0\bytes-1.5.0.jar"
$Sqlite = "$M2\org\xerial\sqlite-jdbc\3.53.2.1\sqlite-jdbc-3.53.2.1.jar"
$FxControls = "$M2\org\openjfx\javafx-controls\$FxVer\javafx-controls-$FxVer-win.jar"
$FxGraphics = "$M2\org\openjfx\javafx-graphics\$FxVer\javafx-graphics-$FxVer-win.jar"
$FxBase = "$M2\org\openjfx\javafx-base\$FxVer\javafx-base-$FxVer-win.jar"
$FxFxml = "$M2\org\openjfx\javafx-fxml\$FxVer\javafx-fxml-$FxVer-win.jar"

$ClientCp = "$Classes;$Gson;$Bcrypt;$Bytes;$Sqlite"
$ModulePath = "$FxControls;$FxGraphics;$FxBase;$FxFxml"

Write-Host "Starting X Clone connected to ArvanCloud (https://x-clone.alirezalotfimoghaddam.ir)..." -ForegroundColor Green

Start-Process -FilePath $Java -ArgumentList @(
    "--module-path", $ModulePath,
    "--add-modules", "javafx.controls,javafx.fxml",
    "-Dxclone.server.url=https://x-clone.alirezalotfimoghaddam.ir",
    "-cp", $ClientCp,
    "client.launcher"
) -WorkingDirectory $PSScriptRoot
