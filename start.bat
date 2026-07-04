@echo off
chcp 65001 >nul
echo ========================================
echo   校园失物招领系统 - 启动中...
echo ========================================
echo.
echo 请稍等，大约需要15秒...
echo 启动成功后浏览器访问: http://localhost:8080
echo.
echo 按 Ctrl+C 可停止应用
echo.

:: 创建上传目录
if not exist "D:\uploads\lost-found" mkdir "D:\uploads\lost-found"

mvn spring-boot:run

pause
