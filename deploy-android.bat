@echo off
setlocal
chcp 65001 >nul 2>&1

:: ============================================================
:: Pegasus Video Player — Build + Deploy su Odin2
:: ============================================================

set DEVICE=69bdb979
set APK_DEBUG=app\build\outputs\apk\debug\app-debug.apk
set APK_RELEASE=app\build\outputs\apk\release\app-release.apk

:: Auto-detect ADB
where adb >nul 2>&1 || (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;%PATH%"
    ) else (
        echo ERRORE: ADB non trovato nel PATH.
        exit /b 1
    )
)

:: Scegli build type (default: debug)
set BUILD_TYPE=debug
if /i "%1"=="release" set BUILD_TYPE=release

echo === Pegasus Video Player — Deploy (%BUILD_TYPE%) ===

:: Build
if "%BUILD_TYPE%"=="release" (
    call gradlew.bat assembleRelease
    set APK=%APK_RELEASE%
) else (
    call gradlew.bat assembleDebug
    set APK=%APK_DEBUG%
)

if errorlevel 1 (
    echo BUILD FAILED
    exit /b 1
)

:: Verifica dispositivo
adb -s %DEVICE% get-state >nul 2>&1
if errorlevel 1 (
    echo ERRORE: Dispositivo %DEVICE% non connesso.
    exit /b 1
)

:: Install (-g = concedi tutti i permessi dichiarati)
adb -s %DEVICE% install -r -g "%APK%"
if errorlevel 1 (
    echo INSTALL FAILED
    exit /b 1
)

echo.
echo Deploy OK
adb -s %DEVICE% shell dumpsys package com.pegasus.videoplayer | findstr versionName
endlocal
