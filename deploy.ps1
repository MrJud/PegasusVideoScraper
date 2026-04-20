# Pegasus Video Player — Copia APK in Downloads (per distribuzione manuale)
param(
    [ValidateSet("debug","release")]
    [string]$BuildType = "debug"
)

$apk  = if ($BuildType -eq "release") {
    "app\build\outputs\apk\release\app-release.apk"
} else {
    "app\build\outputs\apk\debug\app-debug.apk"
}

$dest = "$env:USERPROFILE\Downloads\pegasus-video-$BuildType.apk"

if (-not (Test-Path $apk)) {
    Write-Error "APK non trovato: $apk. Esegui prima deploy-android.bat."
    exit 1
}

Copy-Item $apk $dest -Force
Write-Host "APK copiato in $dest"
