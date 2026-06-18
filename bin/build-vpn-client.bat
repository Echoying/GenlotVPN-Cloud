@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
rem W-01: Release 构建（MSVC + CMake 单配置生成器，自动适配 VS2022/VS2026 等任意版本）
cd /d %~dp0..

set PACKAGE=0
if /i "%~1"=="--package" set PACKAGE=1
if /i "%~1"=="/package" set PACKAGE=1

call bin\check-vpn-client-deps.bat
if errorlevel 1 exit /b 1

rem 未在 VS 命令行里时，自动加载 MSVC 环境（vswhere 探测任意版本）
if "%VSCMD_ARG_TGT_ARCH%"=="" call :InitMSVC

where cl >nul 2>&1
if errorlevel 1 (
  echo.
  echo [X] 未找到 MSVC 编译器 cl。请安装 Visual Studio「使用 C++ 的桌面开发」，
  echo     或改用「x64 Native Tools Command Prompt」运行本脚本。
  exit /b 1
)

if not defined Protobuf_ROOT (
  if exist "D:\anaconda3\Library\include\google\protobuf\message.h" (
    set "Protobuf_ROOT=D:\anaconda3\Library"
  )
)

cd ruoyi-vpn-client
set "BUILD_DIR=build-msvc2022"

rem 选择与 VS 版本无关的单配置生成器：优先 Ninja，其次 JOM，最后 NMake（nmake 随 vcvars 必有）
for %%P in ("%CMAKE_PREFIX_PATH%\..\..") do set "QT_ROOT=%%~fP"
set "GENERATOR=NMake Makefiles"
if exist "%QT_ROOT%\Tools\Ninja\ninja.exe" set "PATH=%QT_ROOT%\Tools\Ninja;%PATH%"
where ninja >nul 2>&1 && set "GENERATOR=Ninja"
if "!GENERATOR!"=="NMake Makefiles" (
  if exist "%QT_ROOT%\Tools\QtCreator\bin\jom\jom.exe" (
    set "PATH=%QT_ROOT%\Tools\QtCreator\bin\jom;%PATH%"
    set "GENERATOR=NMake Makefiles JOM"
  )
)

rem 生成器变更时清理旧 CMake 缓存，避免 generator mismatch
if exist "%BUILD_DIR%\CMakeCache.txt" (
  findstr /C:"CMAKE_GENERATOR:INTERNAL=!GENERATOR!" "%BUILD_DIR%\CMakeCache.txt" >nul 2>&1
  if errorlevel 1 (
    echo [..] 生成器变更，清理 %BUILD_DIR% ...
    rmdir /s /q "%BUILD_DIR%"
  )
)

set "PROTO_ARG="
if defined Protobuf_ROOT set "PROTO_ARG=-DProtobuf_ROOT=%Protobuf_ROOT%"

echo.
echo [..] CMake 配置 ^(!GENERATOR!, Release^) ...
cmake -B %BUILD_DIR% -G "!GENERATOR!" -DCMAKE_BUILD_TYPE=Release -DCMAKE_PREFIX_PATH=%CMAKE_PREFIX_PATH% %PROTO_ARG%
if errorlevel 1 (
  echo.
  echo [X] CMake 配置失败
  echo.
  echo 若已用 Qt Creator Release 构建，可跳过本脚本，直接打包:
  echo   bin\package-vpn-client.bat
  echo.
  echo 若 Protobuf 找不到，可设置 Protobuf_ROOT 或使用 vcpkg toolchain。
  exit /b 1
)

echo [..] Release 编译 ...
cmake --build %BUILD_DIR% --parallel
if errorlevel 1 (
  echo [X] 编译失败
  exit /b 1
)

set "EXE=%BUILD_DIR%\GenlotVPN.exe"
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
echo 打包输出: dist\GenlotVPN-win64-{version}\GenlotVPN-{version}.exe
echo.
endlocal
exit /b 0

:InitMSVC
rem 用 vswhere 自动定位任意版本 VS（2022/2026/…）并加载 x64 环境
set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
if exist "%VSWHERE%" (
  for /f "usebackq tokens=*" %%I in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
    if exist "%%I\VC\Auxiliary\Build\vcvars64.bat" (
      call "%%I\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
      goto :eof
    )
  )
)
rem 回退：扫描已知版本目录（18=VS2026，17=VS2022）
for %%Y in (18 2026 17 2022) do (
  for %%E in (Community Professional Enterprise BuildTools) do (
    if exist "%ProgramFiles%\Microsoft Visual Studio\%%Y\%%E\VC\Auxiliary\Build\vcvars64.bat" (
      call "%ProgramFiles%\Microsoft Visual Studio\%%Y\%%E\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
      goto :eof
    )
  )
)
echo [!] 未能自动加载 MSVC 环境，若编译失败请使用「x64 Native Tools Command Prompt」重试
goto :eof
