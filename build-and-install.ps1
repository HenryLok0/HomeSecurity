# Quick Build & Install Script for HomeSecurity App
# Usage: .\build-and-install.ps1

Write-Host "=== HomeSecurity Build & Install ===" -ForegroundColor Cyan
Write-Host ""

# Set environment
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

# Build
Write-Host "Building APK..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Build successful!" -ForegroundColor Green
    Write-Host ""
    
    # Check device
    Write-Host "Checking connected devices..." -ForegroundColor Yellow
    adb devices
    Write-Host ""
    
    # Install
    Write-Host "Installing to device..." -ForegroundColor Yellow
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ App installed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "To view logs, run:" -ForegroundColor Cyan
        Write-Host "  adb logcat -s HomeSecurityApp:D" -ForegroundColor White
    } else {
        Write-Host "✗ Installation failed" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Build failed" -ForegroundColor Red
}
