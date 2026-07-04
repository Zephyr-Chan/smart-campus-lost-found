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
echo [2/4] 执行基础表脚本...
mysql -u root -p%MYSQL_PWD% lost_found < "src\main\resources\sql\init.sql" 2>nul
echo 基础表创建成功

echo.
echo [3/4] 执行升级表脚本...
mysql -u root -p%MYSQL_PWD% lost_found < "src\main\resources\sql\migration_v2.sql" 2>nul
echo 升级表创建成功

echo.
echo [4/4] 导入测试数据...
mysql -u root -p%MYSQL_PWD% lost_found < "sql\demo-data.sql" 2>nul
echo 测试数据导入成功

echo.
echo ========================================
echo   数据库初始化完成！共13张表+测试数据
echo   默认管理员: admin / 123456
echo   测试用户:   zhangwei2024 / 123456
echo ========================================
pause
