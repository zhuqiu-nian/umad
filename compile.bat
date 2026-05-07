@echo off
cd /d "d:\研究生\软件包\UMAD-master"
echo 正在使用JDK 17编译...
"C:\Program Files\Java\jdk-17\bin\javac" -encoding UTF-8 -sourcepath "src/main/java" -cp "libs/*" -d "target/classes" "src/main/java/app/Application.java"
if exist "target\classes\app\Application.class" (
    echo 编译成功！现在按F5调试
) else (
    echo 编译失败！
)
pause