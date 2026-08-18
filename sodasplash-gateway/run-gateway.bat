@echo off
REM Run Soda Splash API Gateway from JAR file
REM This script runs the built Spring Boot application on port 8080

echo ===================================================
echo Starting Soda Splash API Gateway
echo ===================================================
echo.

REM Get the directory where this script is located
cd /d "%~dp0"

REM Check if the JAR already exists in target
set JAR_EXISTS=0
if exist "target\sodasplash-gateway-0.0.1-SNAPSHOT.jar" set JAR_EXISTS=1

if %JAR_EXISTS%==0 (
    echo JAR not found. Building the gateway...
    echo.
    call mvnw.cmd clean package -DskipTests
    if %errorlevel% neq 0 (
        echo.
        echo BUILD FAILED! Please check the error above.
        pause
        exit /b 1
    )
    echo.
    echo Build complete!
    echo.
)

REM Run the JAR
set JAR_PATH=target\sodasplash-gateway-0.0.1-SNAPSHOT.jar

echo ===================================================
echo Running: %JAR_PATH%
echo ===================================================
echo.
echo Gateway will be available at: http://localhost:8080
echo.
echo Routes configured:
echo   /api/auth/**     -> http://localhost:8081  (Auth Service)
echo   /api/staff/**    -> http://localhost:8081  (Auth Service)
echo   /api/products/** -> http://localhost:8082  (Product Service)
echo   /api/flavours/** -> http://localhost:8082  (Product Service)
echo   /api/orders/**   -> http://localhost:8083  (Order Service)
echo.
echo Press Ctrl+C to stop the gateway
echo ===================================================
echo.

java -jar "%JAR_PATH%"

echo.
echo Gateway stopped.
pause
