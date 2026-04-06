@echo off
cd /d %~dp0
mvn help:effective-pom -q >nul 2>nul
if %ERRORLEVEL% neq 0 (
  echo POM is invalid
  exit /b %ERRORLEVEL%
)
echo POM is valid
