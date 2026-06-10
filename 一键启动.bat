@echo off
title 校园帮 - 一键启动
color 0A
echo ========================================
echo     校园互助跑腿平台 - 一键启动
echo ========================================
echo.
echo 正在启动所有服务，请稍候...
echo.
echo [1/3] 启动 Redis...
start "Redis" "C:\Users\ASUS\Redis\redis-server.exe"
timeout /t 2 /nobreak >nul
echo [2/3] 启动后端...
cd /d C:\Users\ASUS\Desktop\综合实训1
start "后端" cmd /c "java -jar backend\target\campus-help-backend-1.0.0.jar"
timeout /t 5 /nobreak >nul
echo [3/3] 启动管理后台...
start "管理后台" cmd /c "npm --prefix admin run dev"
echo.
echo ========================================
echo 全部启动完成！
echo 管理后台：http://localhost:5173
echo 账号：admin / admin123
echo 微信小程序请在 HBuilderX 中手动启动
echo ========================================
echo.
pause