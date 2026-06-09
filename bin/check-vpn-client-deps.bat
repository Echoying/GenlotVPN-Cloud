@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion
echo ========================================
echo   GenlotVPN Client - Dependency Check
echo ========================================
echo.

set MISSING=0

where cmake >nul 2>&1
if errorlevel 1 (
  echo [X] CMake not found - winget install Kitware.CMake
  set /a MISSING+=1
) else (
  cmake --version 2>nul | findstr /i version
  echo [OK] CMake
)

where protoc >nul 2>&1
if errorlevel 1 (
  echo [X] protoc not found - see ruoyi-vpn-client\docs\WINDOWS_SETUP.md
  set /a MISSING+=1
) else (
  protoc --version 2>nul
  echo [OK] Protobuf
)

if "%CMAKE_PREFIX_PATH%"=="" (
  echo [X] CMAKE_PREFIX_PATH not set - install Qt6 MSVC 64-bit
  echo     Set user env var, then reopen this terminal.
  set /a MISSING+=1
) else if exist "%CMAKE_PREFIX_PATH%\lib\cmake\Qt6\Qt6Config.cmake" (
  if exist "%CMAKE_PREFIX_PATH%\bin\Qt6Core.dll" (
    echo [OK] Qt6 at %CMAKE_PREFIX_PATH%
    if exist "%CMAKE_PREFIX_PATH%\bin\windeployqt.exe" (
      echo [OK] windeployqt
    ) else (
      echo [X] windeployqt.exe missing at %CMAKE_PREFIX_PATH%\bin
      set /a MISSING+=1
    )
  ) else (
    echo [X] Qt6Config found but Qt6Core.dll missing at %CMAKE_PREFIX_PATH%
    echo     Incomplete Qt install - use Qt Maintenance Tool.
    set /a MISSING+=1
  )
) else if exist "%CMAKE_PREFIX_PATH%\config_qtwebengine.summary" (
  echo [X] Only Qt WebEngine addon at %CMAKE_PREFIX_PATH%
  echo     Missing Desktop Qt MSVC kit ^(Core/Quick/Network^).
  echo     Open Qt Maintenance Tool -^> Add -^> Qt 6.11.1 -^> MSVC 2022 64-bit
  set /a MISSING+=1
) else (
  echo [X] Qt6 not found at %CMAKE_PREFIX_PATH%
  echo     Need: lib\cmake\Qt6\Qt6Config.cmake and bin\Qt6Core.dll
  set /a MISSING+=1
)

where cl >nul 2>&1
if errorlevel 1 (
  echo [?] MSVC cl not in PATH - use VS x64 Native Tools prompt
) else (
  echo [OK] MSVC
)

echo.
if %MISSING% GTR 0 (
  echo Missing dependencies. Read: ruoyi-vpn-client\docs\WINDOWS_SETUP.md
  exit /b 1
)
echo All checks passed.
echo   Build:   bin\build-vpn-client.bat
echo   Package: bin\package-vpn-client.bat
echo   Both:    bin\build-vpn-client.bat --package
exit /b 0
