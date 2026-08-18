@echo off
REM Run Soda Splash API Gateway from JAR file
REM This script runs the built Spring Boot application on port 8080

echo ===================================================
echo Starting Soda Splash API Gateway
echo ===================================================
echo.

REM Navigate up to the parent sodasplash-gateway directory where the JAR is
cd /d "%~dp0.."

REM Check if target directory exists
if not exist "target" (
    echo Target directory not found. Building the gateway...
    echo.
    call mvnw clean install -DskipTests
    if %errorlevel% neq 0 (
        echo.
        echo BUILD FAILED!
        pause
        exit /b 1
    )
    echo.
)

REM Navigate to target directory
cd target

REM Find and run the JAR file (excluding the .original file)
echo Looking for JAR file...
echo.

setlocal enabledelayedexpansion
for /f "delims=" %%F in ('dir /b sodasplash-gateway-*.jar 2^>nul') do (
    if "%%F" neq "sodasplash-gateway-0.0.1-SNAPSHOT.jar.original" (
        echo ===================================================
        echo Running: %%F
        echo ===================================================
        echo.
        echo Gateway will be available at: http://localhost:8080
        echo Press Ctrl+C to stop the gateway
        echo.
        echo ===================================================
        echo.
        
        java -jar "%%F"
        goto end
    )
)

echo ERROR: No JAR file found!
echo Please build the gateway first with:
echo   cd ..
echo   mvnw clean install -DskipTests
echo.
pause
exit /b 1

:end
pause
