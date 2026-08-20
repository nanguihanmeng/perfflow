@echo off
chcp 65001 >nul
REM ===========================================================
REM  PerfFlow 后端启动脚本
REM  1. 检查 Java 17 / Maven
REM  2. mvn package（首次或源码变更后）
REM  3. java -jar 启动
REM  Swagger: http://localhost:8080/api/swagger-ui.html
REM  OpenAPI JSON: http://localhost:8080/api/v3/api-docs
REM ===========================================================

setlocal

set "APP_HOME=%~dp0"
cd /d "%APP_HOME%\backend"

if not exist "mvnw.cmd" goto :NO_WRAPPER
echo [INFO] 使用 mvnw 包装器
call mvnw.cmd -q -DskipTests package
goto :RUN

:NO_WRAPPER
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 mvn，请安装 Maven 3.9+ 或在项目中放入 mvnw 包装器
    pause
    exit /b 1
)
mvn -q -DskipTests package

:RUN
if not exist "target\perfflow-backend.jar" (
    echo [ERROR] 编译失败，未生成 target\perfflow-backend.jar
    pause
    exit /b 1
)

set "SPRING_PROFILES_ACTIVE=dev"
java -jar -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE% target\perfflow-backend.jar

endlocal
