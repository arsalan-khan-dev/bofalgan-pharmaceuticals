@echo off
REM ======================================================
REM  Bofalgan Pharmaceuticals - Windows Launch Script
REM  Make sure Java 11+ is installed and JAVA_HOME is set
REM ======================================================

set APP_DIR=%~dp0..
set LIB_DIR=%APP_DIR%\lib
set BUILD_DIR=%APP_DIR%\build

REM Build classpath from all JARs in lib/
set CP=%BUILD_DIR%
for %%f in ("%LIB_DIR%\*.jar") do set CP=%%f;%CP%

java --module-path "%LIB_DIR%" ^
     --add-modules javafx.controls,javafx.graphics,javafx.base ^
     -cp "%CP%" ^
     -Xmx512m ^
     com.bofalgan.pharmacy.Main

pause
