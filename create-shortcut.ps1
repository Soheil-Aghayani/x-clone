# create-shortcut.ps1
# Run once to place an "X Clone" shortcut (with the packaged X Clone icon) on your Desktop.
# Double-click that shortcut to start the backend + client.

$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot
$batFile    = Join-Path $projectDir "launch.bat"
$icoFile    = Join-Path $projectDir "src\main\resources\icons\app\x-clone.ico"
$desktop    = [Environment]::GetFolderPath("Desktop")
$shortcut   = Join-Path $desktop "X Clone.lnk"

if (-not (Test-Path $batFile)) {
    Write-Error "launch.bat not found at: $batFile"
    exit 1
}

if (-not (Test-Path $icoFile)) {
    Write-Host "  X Clone icon not found in resources. Creating a simple X icon instead..."
    $icoFile = Join-Path $projectDir "target\X-Clone.ico"
    New-Item -ItemType Directory -Force -Path (Split-Path $icoFile) | Out-Null

    Add-Type -AssemblyName System.Drawing
    $sizes = @(256, 128, 64, 48, 32, 16)
    $images = New-Object System.Collections.Generic.List[byte[]]
    foreach ($size in $sizes) {
        $bmp = New-Object System.Drawing.Bitmap($size, $size,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.Clear([System.Drawing.Color]::Black)
        $penW = [float]($size * 0.115)
        $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, $penW)
        $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
        $g.DrawLine($pen, [float]($size * 0.18), [float]($size * 0.14),
                          [float]($size * 0.82), [float]($size * 0.86))
        $g.DrawLine($pen, [float]($size * 0.82), [float]($size * 0.14),
                          [float]($size * 0.18), [float]($size * 0.86))
        $pen.Dispose(); $g.Dispose()
        $ms = New-Object System.IO.MemoryStream
        $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $images.Add($ms.ToArray())
        $ms.Dispose(); $bmp.Dispose()
    }
    $out = New-Object System.IO.MemoryStream
    $w = New-Object System.IO.BinaryWriter($out)
    $w.Write([uint16]0); $w.Write([uint16]1); $w.Write([uint16]$sizes.Count)
    $offset = 6 + 16 * $sizes.Count
    for ($i = 0; $i -lt $sizes.Count; $i++) {
        $sz = $sizes[$i]; $data = $images[$i]
        $d = if ($sz -ge 256) { 0 } else { $sz }
        $w.Write([byte]$d); $w.Write([byte]$d)
        $w.Write([byte]0); $w.Write([byte]0)
        $w.Write([uint16]1); $w.Write([uint16]32)
        $w.Write([uint32]$data.Length); $w.Write([uint32]$offset)
        $offset += $data.Length
    }
    foreach ($data in $images) { $w.Write($data) }
    $w.Flush()
    [System.IO.File]::WriteAllBytes($icoFile, $out.ToArray())
    $out.Dispose()
    Write-Host "  Fallback icon written: $icoFile"
}

$wsh = New-Object -ComObject WScript.Shell
$lnk = $wsh.CreateShortcut($shortcut)
$lnk.TargetPath       = $batFile
$lnk.WorkingDirectory = $projectDir
$lnk.IconLocation     = "$icoFile,0"
$lnk.Description      = "Launch X Clone (server + client)"
$lnk.WindowStyle      = 1
$lnk.Save()

Write-Host ""
Write-Host "  Shortcut created: $shortcut"
Write-Host "  Icon:             $icoFile"
Write-Host "  Target:           $batFile"
Write-Host ""
Write-Host "  Double-click 'X Clone' on your Desktop to launch the app."
Write-Host "  Keep the black console window open while you use the app."
