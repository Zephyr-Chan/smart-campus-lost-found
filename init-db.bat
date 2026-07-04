@echo off
chcp 65001 >nul
echo ========================================
echo   校园失物招领系统 - 数据库初始化
echo ========================================
echo.

set /p MYSQL_PWD=请输入MySQL root密码：

echo.
echo [1/3] 创建数据库...
mysql -u root -p%MYSQL_PWD% -e "CREATE DATABASE IF NOT EXISTS lost_found DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if %errorlevel% neq 0 (
    echo 数据库创建失败，请检查MySQL是否启动以及密码是否正确
    pause
    exit /b 1
)
echo 数据库创建成功

echo.
echo [2/3] 执行基础表脚本...
mysql -u root -p%MYSQL_PWD% lost_found < "src\main\resources\sql\init.sql" 2>nul
echo 基础表创建成功

echo.
echo [3/3] 执行升级表脚本...
mysql -u root -p%MYSQL_PWD% lost_found < "src\main\resources\sql\migration_v2.sql" 2>nul
echo 升级表创建成功

echo.
echo ========================================
echo   数据库初始化完成！共13张表
echo   默认管理员: admin / debug123
echo ========================================
pause
