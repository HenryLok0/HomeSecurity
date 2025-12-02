# Android Studio HomeSecurity - Environment Setup
# Run this before using gradlew or adb commands

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\tools;$env:Path"

Write-Host "✓ JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green
Write-Host "✓ ANDROID_HOME set to: $env:ANDROID_HOME" -ForegroundColor Green
Write-Host "✓ PATH updated with Java and Android SDK tools" -ForegroundColor Green
Write-Host ""
Write-Host "Java version:" -ForegroundColor Cyan
java -version
Write-Host ""
Write-Host "ADB version:" -ForegroundColor Cyan
adb version
Write-Host ""
Write-Host "Environment ready! You can now run:" -ForegroundColor Yellow
Write-Host "  .\gradlew.bat assembleDebug" -ForegroundColor White
Write-Host "  adb install -r app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
