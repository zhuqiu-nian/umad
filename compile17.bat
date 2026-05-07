@echo off
cd /d "d:\研究生\软件包\UMAD-master"
echo 正在使用 JDK 17 编译...

"C:\Program Files\Java\jdk-17\bin\javac" -encoding UTF-8 -cp "libs/*" -d target/classes src\main\java\app\Application.java

echo.
if exist target\classes\app\Application.class (
    echo ========================================
    echo 编译成功！按 F5 调试
    echo ========================================
) else (
    echo 编译失败
)
pause