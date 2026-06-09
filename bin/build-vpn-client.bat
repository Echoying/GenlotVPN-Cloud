@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
rem W-01: Release 构建（MSVC 2022 + CMake，固定输出 build-msvc2022\Release\）
cd /d %~dp0..

set PACKAGE=0
if /i "%~1"=="--package" set PACKAGE=1
if /i "%~1"=="/package" set PACKAGE=1

call bin\check-vpn-client-deps.bat
if errorlevel 1 exit /b 1

if "%VSCMD_ARG_TGT_ARCH%"=="" call :InitMSVC

if not defined Protobuf_ROOT (
  if exist "D:\anaconda3\Library\include\google\protobuf\message.h" (
    set "Protobuf_ROOT=D:\anaconda3\Library"
  )
)

cd ruoyi-vpn-client
set "BUILD_DIR=build-msvc2022"

set "CMAKE_ARGS=-B %BUILD_DIR% -DCMAKE_PREFIX_PATH=%CMAKE_PREFIX_PATH%"
if defined Protobuf_ROOT set "CMAKE_ARGS=!CMAKE_ARGS! -DProtobuf_ROOT=%Protobuf_ROOT%"

echo.
echo [..] CMake 配置 ^(Visual Studio 2022 x64^) ...
cmake !CMAKE_ARGS! -G "Visual Studio 17 2022" -A x64
if errorlevel 1 (
  echo.
  echo [X] CMake 配置失败（需安装 Visual Studio 2022「使用 C++ 的桌面开发」）
  echo.
  echo 若已用 Qt Creator Release 构建，可跳过本脚本，直接打包:
  echo   bin\package-vpn-client.bat
  echo.
  echo 若 Protobuf 找不到，可设置 Protobuf_ROOT 或使用 vcpkg toolchain。
  exit /b 1
)

echo [..] Release 编译 ...
cmake --build %BUILD_DIR% --config Release --parallel
if errorlevel 1 (
  echo [X] 编译失败
  exit /b 1
)

set "EXE=%BUILD_DIR%\Release\GenlotVPN.exe"
if not exist "%EXE%" (
  echo [X] 未生成 %EXE%
  exit /b 1
)

echo.
echo [OK] 构建完成: %CD%\%EXE%
cd ..

if "%PACKAGE%"=="1" (
  call bin\package-vpn-client.bat
  exit /b !errorlevel!
)

echo.
echo 下一步打包依赖 ^(windeployqt^):
echo   bin\package-vpn-client.bat
echo 或一步构建并打包:
echo   bin\build-vpn-client.bat --package
echo.
endlocal
exit /b 0

:InitMSVC
for %%E in (Community Professional Enterprise BuildTools) do (
  if exist "%ProgramFiles%\Microsoft Visual Studio\2022\%%E\VC\Auxiliary\Build\vcvars64.bat" (
    call "%ProgramFiles%\Microsoft Visual Studio\2022\%%E\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
    goto :eof
  )
)
echo [!] 未加载 MSVC 环境，若编译失败请使用「x64 Native Tools Command Prompt」重试
goto :eof
