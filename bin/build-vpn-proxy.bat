@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
rem Release 构建 GenlotVPN-Proxy（MSVC + Qt6，无需 Protobuf）
cd /d %~dp0..

set PACKAGE=0
if /i "%~1"=="--package" set PACKAGE=1
if /i "%~1"=="/package" set PACKAGE=1

if "%CMAKE_PREFIX_PATH%"=="" (
  echo [X] 未设置 CMAKE_PREFIX_PATH
  echo     示例: set CMAKE_PREFIX_PATH=D:\apps\Qt\6.11.1\msvc2022_64
  exit /b 1
)

:TrimQtPath
if "%CMAKE_PREFIX_PATH:~-1%"==" " set "CMAKE_PREFIX_PATH=%CMAKE_PREFIX_PATH:~0,-1%" & goto TrimQtPath

if "%VSCMD_ARG_TGT_ARCH%"=="" call :InitMSVC

where cl >nul 2>&1
if errorlevel 1 (
  echo [X] 未找到 MSVC 编译器 cl
  exit /b 1
)

cd ruoyi-vpn-proxy
set "BUILD_DIR=build-msvc2022"

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

if exist "%BUILD_DIR%\CMakeCache.txt" (
  findstr /C:"CMAKE_GENERATOR:INTERNAL=!GENERATOR!" "%BUILD_DIR%\CMakeCache.txt" >nul 2>&1
  if errorlevel 1 (
    echo [..] 生成器变更，清理 %BUILD_DIR% ...
    rmdir /s /q "%BUILD_DIR%"
  )
)

echo.
echo [..] CMake 配置 ^(!GENERATOR!, Release^) ...
cmake -B %BUILD_DIR% -G "!GENERATOR!" -DCMAKE_BUILD_TYPE=Release -DCMAKE_PREFIX_PATH=%CMAKE_PREFIX_PATH%
if errorlevel 1 (
  echo [X] CMake 配置失败
  exit /b 1
)

echo [..] Release 编译 ...
cmake --build %BUILD_DIR% --parallel
if errorlevel 1 (
  echo [X] 编译失败
  exit /b 1
)

set "EXE=%BUILD_DIR%\GenlotVPN-Proxy.exe"
if not exist "%EXE%" (
  echo [X] 未生成 %EXE%
  exit /b 1
)

echo.
echo [OK] 构建完成: %CD%\%EXE%
cd ..

if "%PACKAGE%"=="1" (
  call bin\package-vpn-proxy.bat
  exit /b !errorlevel!
)

echo.
echo 下一步打包: bin\package-vpn-proxy.bat
echo 或一步构建并打包: bin\build-vpn-proxy.bat --package
echo.
endlocal
exit /b 0

:InitMSVC
set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
if exist "%VSWHERE%" (
  for /f "usebackq tokens=*" %%I in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
    if exist "%%I\VC\Auxiliary\Build\vcvars64.bat" (
      call "%%I\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
      goto :eof
    )
  )
)
for %%Y in (18 2026 17 2022) do (
  for %%E in (Community Professional Enterprise BuildTools) do (
    if exist "%ProgramFiles%\Microsoft Visual Studio\%%Y\%%E\VC\Auxiliary\Build\vcvars64.bat" (
      call "%ProgramFiles%\Microsoft Visual Studio\%%Y\%%E\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
      goto :eof
    )
  )
)
goto :eof
