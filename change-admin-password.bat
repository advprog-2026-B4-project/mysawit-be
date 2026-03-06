@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%" >nul

if "%~1"=="" (
  echo Usage: change-admin-password.bat ^<newPassword^>
  exit /b 1
)

if exist ".env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
    if not "%%A"=="" set "%%A=%%B"
  )
) else (
  echo [WARN] .env not found. Using default DB connection settings.
)

call ".\gradlew.bat" changeAdminPassword -Ppassword="%~1"
set "EXIT_CODE=%ERRORLEVEL%"

popd >nul
exit /b %EXIT_CODE%
