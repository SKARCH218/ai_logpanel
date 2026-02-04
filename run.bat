@echo off
chcp 65001 > nul
echo.
echo ╔═══════════════════════════════════════════════╗
echo ║    AI Log Panel - 개발 모드 실행                ║
echo ╚═══════════════════════════════════════════════╝
echo.
echo 🚀 애플리케이션을 실행합니다...
echo.
call gradlew.bat :composeApp:run
