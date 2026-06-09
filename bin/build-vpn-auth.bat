@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0.."

echo ========================================
echo   构建 ruoyi-vpn-auth（含 TCP Protobuf）
echo ========================================
echo.

call mvn clean package -pl ruoyi-vpn-protocol,ruoyi-vpn-auth -am -DskipTests
if errorlevel 1 (
  echo [X] Maven 构建失败
  exit /b 1
)

set JAR=ruoyi-vpn-auth\target\ruoyi-vpn-auth.jar
jar tf "%JAR%" | findstr /i "BOOT-INF/lib/ruoyi-vpn-protocol" >nul
if errorlevel 1 (
  echo [X] fat jar 未包含 ruoyi-vpn-protocol，请检查 pom 依赖
  exit /b 1
)

echo [OK] %JAR%
echo [OK] 已包含 ruoyi-vpn-protocol
echo.
echo 部署: 将上述 jar 上传到服务器后 java -jar ruoyi-vpn-auth.jar
echo Docker: cd docker ^&^& python copy.py ^&^& docker-compose up -d --build ruoyi-vpn-auth
exit /b 0
