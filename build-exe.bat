@echo off
chcp 65001 > nul
echo.
echo ╔═══════════════════════════════════════════════╗
echo ║      AI Log Panel - EXE 빌드 도구               ║
echo ╚═══════════════════════════════════════════════╝
echo.
echo [1/4] 아이콘 변환 중...
python convert-icon.py
if %ERRORLEVEL% NEQ 0 (
    echo ⚠ 아이콘 변환 실패. 기존 아이콘 사용
)

echo.
echo [2/4] 빌드 캐시 정리 중...
call gradlew.bat clean

echo.
echo [3/4] Windows EXE 빌드 중...
call gradlew.bat :composeApp:packageExe

echo.
echo [4/4] 빌드 완료!
echo.
echo 📦 생성된 파일:
dir /B composeApp\build\compose\binaries\main\exe\*.exe 2>nul
echo.
echo 📂 위치: composeApp\build\compose\binaries\main\exe\
echo.
echo 🎉 빌드가 완료되었습니다!
echo.
pause
