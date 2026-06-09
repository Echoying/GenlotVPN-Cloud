@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "REPO_ROOT=%SCRIPT_DIR%..\.."
set "CRT=%REPO_ROOT%\docker\ruoyi\vpn\auth\certs\server.crt"

if not "%~1"=="" set "CRT=%~1"

if not exist "%CRT%" (
  echo 错误: 证书不存在: %CRT%
  exit /b 1
)

where openssl >nul 2>&1
if errorlevel 1 (
  echo 错误: 未找到 openssl
  exit /b 1
)

for /f "tokens=2 delims= " %%H in ('openssl x509 -in "%CRT%" -pubkey -noout ^| openssl pkey -pubin -outform der ^| openssl dgst -sha256 ^| findstr /i "stdin"') do set "PIN=%%H"

echo certPinSha256: %PIN%
echo.
echo 粘贴到 config.json 或设置页「证书指纹」字段

endlocal
