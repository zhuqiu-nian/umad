@echo off
REM RGH索引测试 - pn=2, pbn=2 (固定)
REM 对比表1、表2、表3

echo ==== clusteredvector数据集 - RGH索引测试 ====
echo.
printf -- "-tm CRGH -t vector -i rgh -f data/vector/clusteredvector-2d-100k-100c.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm CRGH -tbn vector-LOCAL-5000-rgh-FFT-numPivots2-RGH-numPartition2-maxLeafSize20 -qf data/vector/clusteredvector-2d-100k-100c.txt -m batch -r 0.05 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.
echo ==== hawii数据集 - RGH索引测试 ====
echo.
printf -- "-tm HRGH -t vector -i rgh -f data/vector/hawii.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm HRGH -tbn vector-LOCAL-5000-rgh-FFT-numPivots2-RGH-numPartition2-maxLeafSize20 -qf data/vector/hawii.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.
echo ==== texas数据集 - RGH索引测试 ====
echo.
printf -- "-tm TRGH -t vector -i rgh -f data/vector/texas.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm TRGH -tbn vector-LOCAL-5000-rgh-FFT-numPivots2-RGH-numPartition2-maxLeafSize20 -qf data/vector/texas.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.
echo ==== Uniform数据集 - RGH索引测试 ====
echo.
printf -- "-tm URGH -t vector -i rgh -f data/vector/Uniform-20-d-vector.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm URGH -tbn vector-LOCAL-5000-rgh-FFT-numPivots2-RGH-numPartition2-maxLeafSize20 -qf data/vector/Uniform-20-d-vector.txt -m batch -r 0.1 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo RGH测试完成！
pause