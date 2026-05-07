@echo off
cd /d "d:\研究生\软件包\UMAD-master"
echo Compiling all Java files...

dir /s /b src\main\java\*.java | findstr /V "Voronoi" > java_files.txt

"C:\Program Files\Java\jdk-16.0.2\bin\javac" -encoding UTF-8 -cp "libs/*" -d target/classes @java_files.txt

echo.
echo ========================
if exist target\classes\app\Application.class (
    echo SUCCESS!
) else (
    echo FAILED!
)
echo ========================
pause