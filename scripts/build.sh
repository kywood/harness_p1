#!/usr/bin/env bash
set -e

powershell.exe -NoProfile -NonInteractive -Command '
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location "D:\project\wood\harness_p1\android"
& .\gradlew.bat assembleDebug --console=plain
exit $LASTEXITCODE
'
