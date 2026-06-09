@echo off
setlocal
cd /d %~dp0..
call bin\check-vpn-client-deps.bat
if errorlevel 1 exit /b 1

cd ruoyi-vpn-client

if not exist build mkdir build
cmake -B build -DCMAKE_PREFIX_PATH=%CMAKE_PREFIX_PATH%
if errorlevel 1 (
  echo.
  echo 若 Protobuf 找不到，可尝试 vcpkg:
  echo   cmake -B build -DCMAKE_PREFIX_PATH=%CMAKE_PREFIX_PATH% -DCMAKE_TOOLCHAIN_FILE=C:\vcpkg\scripts\buildsystems\vcpkg.cmake
  exit /b 1
)

cmake --build build --config Release
if errorlevel 1 exit /b 1

echo.
echo 构建完成。若首次运行缺少 DLL，请执行:
echo   windeployqt build\Release\GenlotVPN.exe
echo.
endlocal
