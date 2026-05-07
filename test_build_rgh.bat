@echo off
REM Build RGH index
echo -bi
echo CRGH
echo data/vector/clusteredvector-2d-100k-100c.txt
echo 5000
echo 2
echo rgh
echo FFT
echo vector
echo 20
echo local
) | java -cp "target/classes;libs/colt-1.2.0.jar;libs/Jama-1.0.3.jar;libs/mckoi-1.0.jar" app.Application

pause