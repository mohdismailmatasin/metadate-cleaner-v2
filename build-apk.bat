@echo off
setlocal enabledelayedexpansion

set "GRADLE=%USERPROFILE%\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat"

echo ============================================
echo    Metadata Cleaner - APK Build Script
echo ============================================
echo.
echo [1] Build Debug APK
echo [2] Build Release APK
echo [3] Build Both (Debug + Release)
echo [4] Clean Build
echo [5] Exit
echo.

set /p choice="Select option (1-5): "

if "%choice%"=="1" goto debug
if "%choice%"=="2" goto release
if "%choice%"=="3" goto both
if "%choice%"=="4" goto clean
if "%choice%"=="5" goto end

echo Invalid option!
pause
goto end

:debug
echo.
echo Building Debug APK...
call "%GRADLE%" assembleDebug
if %errorlevel% neq 0 (
    echo Debug build FAILED!
    pause
    goto end
)
echo Debug APK: app\build\outputs\apk\debug\app-debug.apk
pause
goto end

:release
echo.
echo Building Release APK...
call "%GRADLE%" assembleRelease
if %errorlevel% neq 0 (
    echo Release build FAILED!
    pause
    goto end
)
echo Release APK: app\build\outputs\apk\release\app-release.apk
pause
goto end

:both
echo.
echo Building Debug APK...
call "%GRADLE%" assembleDebug
if %errorlevel% neq 0 (
    echo Debug build FAILED!
    pause
    goto end
)
echo Debug APK: app\build\outputs\apk\debug\app-debug.apk
echo.
echo Building Release APK...
call "%GRADLE%" assembleRelease
if %errorlevel% neq 0 (
    echo Release build FAILED!
    pause
    goto end
)
echo Release APK: app\build\outputs\apk\release\app-release.apk
pause
goto end

:clean
echo.
echo Cleaning build...
call "%GRADLE%" clean
pause
goto end

:end
endlocal
