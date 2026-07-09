@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
rem windeployqt 打包 GenlotVPN-Proxy（无需 Protobuf）
cd /d %~dp0..\ruoyi-vpn-proxy

if "%CMAKE_PREFIX_PATH%"=="" (
  echo [X] 未设置 CMAKE_PREFIX_PATH
  echo     示例: set CMAKE_PREFIX_PATH=D:\apps\Qt\6.11.1\msvc2022_64
  exit /b 1
)

:TrimQtPath
if "%CMAKE_PREFIX_PATH:~-1%"==" " set "CMAKE_PREFIX_PATH=%CMAKE_PREFIX_PATH:~0,-1%" & goto TrimQtPath

set "WINDEPLOYQT=%CMAKE_PREFIX_PATH%\bin\windeployqt.exe"
if not exist "%WINDEPLOYQT%" (
  echo [X] 未找到 windeployqt: %WINDEPLOYQT%
  exit /b 1
)

set "EXE_PATH="
if defined GENLOT_PROXY_EXE_PATH if exist "%GENLOT_PROXY_EXE_PATH%" set "EXE_PATH=%GENLOT_PROXY_EXE_PATH%"
if not defined EXE_PATH if exist "build-msvc2022\GenlotVPN-Proxy.exe" set "EXE_PATH=build-msvc2022\GenlotVPN-Proxy.exe"
if not defined EXE_PATH if exist "build-msvc2022\Release\GenlotVPN-Proxy.exe" set "EXE_PATH=build-msvc2022\Release\GenlotVPN-Proxy.exe"
if not defined EXE_PATH (
  for /d %%D in (build\Desktop_Qt_*-*Release build\Desktop_Qt_*_Release) do (
    if exist "%%D\GenlotVPN-Proxy.exe" set "EXE_PATH=%%D\GenlotVPN-Proxy.exe"
  )
)
if not defined EXE_PATH (
  echo [X] 未找到 Release GenlotVPN-Proxy.exe，请先运行 bin\build-vpn-proxy.bat
  exit /b 1
)

echo %EXE_PATH% | findstr /I /C:"\Debug\" /C:"-Debug" /C:"_Debug" >nul
if not errorlevel 1 (
  echo [X] 拒绝打包 Debug 构建: %EXE_PATH%
  exit /b 1
)

echo [OK] Release exe: %EXE_PATH%

set "GENLOT_VER="
for /f "tokens=3 delims= " %%V in ('findstr /C:"project(GenlotVPN-Proxy VERSION" CMakeLists.txt') do set "GENLOT_VER=%%V"
if not defined GENLOT_VER set "GENLOT_VER=1.0.0"
set "PKG_EXE=GenlotVPN-Proxy-%GENLOT_VER%.exe"
set "DIST=dist\GenlotVPN-Proxy-win64-%GENLOT_VER%"
echo [OK] 打包版本: %GENLOT_VER% ^(%PKG_EXE%^)

if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%" 2>nul

copy /y "%EXE_PATH%" "%DIST%\%PKG_EXE%" >nul
if errorlevel 1 (
  echo [X] 复制 exe 失败
  exit /b 1
)

set "QML_MODULE_SRC="
for %%F in ("%EXE_PATH%") do set "EXE_DIR=%%~dpF"
if exist "!EXE_DIR!GenlotVPNProxy\qmldir" set "QML_MODULE_SRC=!EXE_DIR!GenlotVPNProxy"
if not defined QML_MODULE_SRC if exist "build-msvc2022\GenlotVPNProxy\qmldir" set "QML_MODULE_SRC=build-msvc2022\GenlotVPNProxy"
if not defined QML_MODULE_SRC (
  for /d %%D in (build\Desktop_Qt_*-*Release build\Desktop_Qt_*_Release) do (
    if exist "%%D\GenlotVPNProxy\qmldir" set "QML_MODULE_SRC=%%D\GenlotVPNProxy"
  )
)
if not defined QML_MODULE_SRC (
  echo [X] 未找到 GenlotVPNProxy QML 模块，请先 Release 构建
  exit /b 1
)
if exist "%DIST%\GenlotVPNProxy" rmdir /s /q "%DIST%\GenlotVPNProxy"
xcopy /E /I /Y "!QML_MODULE_SRC!" "%DIST%\GenlotVPNProxy" >nul
echo [OK] GenlotVPNProxy QML 模块

set "QMLDIR=%CD%\qml"
echo [..] 运行 windeployqt ...
pushd "%DIST%"
"%WINDEPLOYQT%" --release --compiler-runtime --qmldir "%QMLDIR%" "%PKG_EXE%"
set "WDQ_ERR=!errorlevel!"
popd
if !WDQ_ERR! neq 0 (
  echo [X] windeployqt 失败
  exit /b 1
)
if exist "%DIST%\Qt6Cored.dll" (
  echo [X] 发现 Qt6Cored.dll，Debug/Release 不匹配
  exit /b 1
)
echo [OK] windeployqt

copy /y "resources\config.default.json" "%DIST%\config.default.json" >nul
copy /y "resources\config.json" "%DIST%\config.json" >nul
echo [OK] 配置文件

echo.
echo ========================================
echo   打包完成: %CD%\%DIST%
echo ========================================
endlocal
exit /b 0
