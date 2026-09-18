@echo off
set JAVA_HOME=C:\Android\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "C:\Users\Admin\Desktop\AirMouse\AirMouse"
call gradlew.bat assembleDebug --no-daemon --stacktrace
echo EXIT_CODE=%ERRORLEVEL%
