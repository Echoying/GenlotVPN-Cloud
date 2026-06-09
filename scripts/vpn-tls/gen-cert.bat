@echo off
setlocal EnableDelayedExpansion

REM 生成 VPN TCP 自签 TLS 证书（默认 SAN IP:10.9.2.177）
set "SCRIPT_DIR=%~dp0"
set "REPO_ROOT=%SCRIPT_DIR%..\.."
set "OUT_DIR=%REPO_ROOT%\docker\ruoyi\vpn\auth\certs"
set "SAN_IP=10.9.2.177"

if not "%~1"=="" set "OUT_DIR=%~1"
if not "%~2"=="" set "SAN_IP=%~2"

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

if exist "%OUT_DIR%\server.crt" (
  echo 错误: 已存在 server.crt，请先备份后删除
  exit /b 1
)
if exist "%OUT_DIR%\server.key" (
  echo 错误: 已存在 server.key，请先备份后删除
  exit /b 1
)

where openssl >nul 2>&1
if errorlevel 1 (
  echo 错误: 未找到 openssl，请安装 OpenSSL 并加入 PATH
  exit /b 1
)

set "OPENSSL_CONF=%SCRIPT_DIR%openssl-min.cnf"
set "ALT_CONF=%TEMP%\genlot-vpn-alt-%RANDOM%.cnf"
(
  echo [req]
  echo distinguished_name = req_distinguished_name
  echo x509_extensions = v3_req
  echo prompt = no
  echo.
  echo [req_distinguished_name]
  echo.
  echo [v3_req]
  echo subjectAltName = IP:%SAN_IP%
) > "%ALT_CONF%"

openssl req -x509 -newkey rsa:2048 -nodes ^
  -keyout "%OUT_DIR%\server.key" -out "%OUT_DIR%\server.crt" -days 3650 ^
  -subj "/CN=GenlotVPN/O=Genlot/C=CN" ^
  -config "%ALT_CONF%"

del "%ALT_CONF%" 2>nul

if errorlevel 1 exit /b 1

echo 证书已生成:
echo   %OUT_DIR%\server.crt
echo   %OUT_DIR%\server.key
echo.
echo 导出 Pin 指纹:
call "%SCRIPT_DIR%export-pin.bat" "%OUT_DIR%\server.crt"

endlocal
