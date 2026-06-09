@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
rem W-02: windeployqt + Qt/Protobuf -> dist\GenlotVPN-win64
cd /d %~dp0..\ruoyi-vpn-client

if "%CMAKE_PREFIX_PATH%"=="" (
  echo [X] CMAKE_PREFIX_PATH not set
  echo     Example: set CMAKE_PREFIX_PATH=D:\apps\Qt\6.11.1\msvc2022_64
  exit /b 1
)

set "WINDEPLOYQT=%CMAKE_PREFIX_PATH%\bin\windeployqt.exe"
if not exist "%WINDEPLOYQT%" (
  echo [X] windeployqt not found: %WINDEPLOYQT%
  exit /b 1
)

rem Optional: set GENLOT_EXE_PATH to a specific Release exe
set "EXE_PATH="
if defined GENLOT_EXE_PATH if exist "%GENLOT_EXE_PATH%" set "EXE_PATH=%GENLOT_EXE_PATH%"

if not defined EXE_PATH if exist "build-msvc2022\Release\GenlotVPN.exe" set "EXE_PATH=build-msvc2022\Release\GenlotVPN.exe"
if not defined EXE_PATH if exist "build\Release\GenlotVPN.exe" set "EXE_PATH=build\Release\GenlotVPN.exe"
if not defined EXE_PATH (
  for /d %%D in (build\Desktop_Qt_*_MSVC*) do (
    if exist "%%D\Release\GenlotVPN.exe" set "EXE_PATH=%%D\Release\GenlotVPN.exe"
  )
)
rem Qt Creator shadow build: ...\Desktop_Qt_*_64bit-Release\GenlotVPN.exe
if not defined EXE_PATH (
  for /d %%D in (build\Desktop_Qt_*-*Release build\Desktop_Qt_*_Release) do (
    if exist "%%D\GenlotVPN.exe" set "EXE_PATH=%%D\GenlotVPN.exe"
  )
)

if not defined EXE_PATH (
  echo [X] Release GenlotVPN.exe not found
  echo.
  echo     Do NOT package Debug builds - clean machines will crash with:
  echo       "QML debugging is enabled"
  echo.
  echo     Qt Creator: switch build type to Release, rebuild, then run this script again
  echo     Or:        bin\build-vpn-client.bat --package
  echo.
  echo     Expected paths examples:
  echo       build\Desktop_Qt_*_64bit-Release\GenlotVPN.exe
  echo       build-msvc2022\Release\GenlotVPN.exe
  exit /b 1
)

echo %EXE_PATH% | findstr /I /C:"\Debug\" /C:"-Debug" /C:"_Debug" >nul
if not errorlevel 1 (
  echo [X] Debug build rejected: %EXE_PATH%
  exit /b 1
)

echo [OK] Release exe: %EXE_PATH%

set "DIST=dist\GenlotVPN-win64"
if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%" 2>nul

copy /y "%EXE_PATH%" "%DIST%\GenlotVPN.exe" >nul
if errorlevel 1 (
  echo [X] Failed to copy exe
  exit /b 1
)

rem QML module metadata (qmldir + pages) must sit next to exe for custom QML types
set "QML_MODULE_SRC="
for %%F in ("%EXE_PATH%") do set "EXE_DIR=%%~dpF"
if exist "!EXE_DIR!GenlotVPN\qmldir" set "QML_MODULE_SRC=!EXE_DIR!GenlotVPN"
if not defined QML_MODULE_SRC if exist "build-msvc2022\Release\GenlotVPN\qmldir" set "QML_MODULE_SRC=build-msvc2022\Release\GenlotVPN"
if not defined QML_MODULE_SRC (
  for /d %%D in (build\Desktop_Qt_*-*Release build\Desktop_Qt_*_Release) do (
    if exist "%%D\GenlotVPN\qmldir" set "QML_MODULE_SRC=%%D\GenlotVPN"
  )
)
if not defined QML_MODULE_SRC (
  echo [X] GenlotVPN QML module not found - rebuild Release first
  exit /b 1
)
if exist "%DIST%\GenlotVPN" rmdir /s /q "%DIST%\GenlotVPN"
xcopy /E /I /Y "!QML_MODULE_SRC!" "%DIST%\GenlotVPN" >nul
if errorlevel 1 (
  echo [X] Failed to copy GenlotVPN QML module
  exit /b 1
)
if not exist "%DIST%\GenlotVPN\qmldir" (
  echo [X] Missing GenlotVPN\qmldir in package output
  exit /b 1
)
echo [OK] GenlotVPN QML module

set "QMLDIR=%CD%\qml"
echo [..] Running windeployqt ...
pushd "%DIST%"
"%WINDEPLOYQT%" --release --compiler-runtime --qmldir "%QMLDIR%" GenlotVPN.exe
set "WDQ_ERR=!errorlevel!"
popd
if !WDQ_ERR! neq 0 (
  echo [X] windeployqt failed
  exit /b 1
)
if exist "%DIST%\Qt6Cored.dll" (
  echo [X] Found Qt6Cored.dll - Debug/Release mismatch
  exit /b 1
)
if not exist "%DIST%\Qt6Core.dll" (
  echo [X] Missing Qt6Core.dll after windeployqt
  exit /b 1
)
echo [OK] windeployqt (Release)

set "PROTO_BIN="
if defined Protobuf_ROOT if exist "%Protobuf_ROOT%\bin\libprotobuf.dll" set "PROTO_BIN=%Protobuf_ROOT%\bin\"
if not defined PROTO_BIN if exist "D:\anaconda3\Library\bin\libprotobuf.dll" set "PROTO_BIN=D:\anaconda3\Library\bin\"
if not defined PROTO_BIN (
  for %%F in ("%EXE_PATH%") do (
    if exist "%%~dpFlibprotobuf.dll" set "PROTO_BIN=%%~dpF"
  )
)
set "PROTO_COPIED=0"
if defined PROTO_BIN (
  for %%D in (libprotobuf.dll zlib.dll abseil_dll.dll) do (
    if exist "!PROTO_BIN!%%D" (
      copy /y "!PROTO_BIN!%%D" "%DIST%\%%D" >nul
      if errorlevel 1 (
        echo [X] Failed to copy %%D
        exit /b 1
      )
      echo       %%D
      set "PROTO_COPIED=1"
    )
  )
)
if "!PROTO_COPIED!"=="0" (
  echo [X] libprotobuf.dll not copied
  echo     Set Protobuf_ROOT, e.g. set Protobuf_ROOT=D:\anaconda3\Library
  exit /b 1
)
echo [OK] Protobuf runtime DLLs

copy /y "resources\config.default.json" "%DIST%\config.default.json" >nul
if not exist "%DIST%\config.default.json" (
  echo [X] Missing config.default.json in package output
  exit /b 1
)
echo [OK] config files

echo.
echo ========================================
echo   Package ready: %CD%\%DIST%
echo ========================================
echo.
if not exist "%DIST%\libprotobuf.dll" (
  echo [X] Missing libprotobuf.dll in output
  exit /b 1
)
if not exist "%DIST%\zlib.dll" (
  echo [!] Missing zlib.dll - copy from Protobuf_ROOT\bin if runtime fails
)
dir /b "%DIST%\GenlotVPN.exe" "%DIST%\Qt6Core.dll" "%DIST%\libprotobuf.dll" "%DIST%\zlib.dll" 2>nul
endlocal
exit /b 0
