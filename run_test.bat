@echo off
REM 完整测试：4个数据集 x 5种索引
REM 新的索引参数：VP(pn=1,pbn=2), VP(pn=2,pbn=2), VP(pn=2,pbn=3)

echo ==== clusteredvector数据集测试 ====
echo.
echo ---- GH索引 ----
printf -- "-tm CGH -t vector -i gh -f data/vector/clusteredvector-2d-100k-100c.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm CGH -tbn vector-LOCAL-5000-gh-FFT-GH-numPartitionnull-maxLeafSize20 -qf data/vector/clusteredvector-2d-100k-100c.txt -m batch -r 0.05 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=1,pbn=2)索引 ----
printf -- "-tm CVP12 -t vector -i vp -pn 1 -pbn 2 -f data/vector/clusteredvector-2d-100k-100c.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
echo 索引名: vector-LOCAL-5000-vp-FFT-numPivots1-BALANCED-numPartition2-maxLeafSize20
printf -- "-tm CVP12 -tbn vector-LOCAL-5000-vp-FFT-numPivots1-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/clusteredvector-2d-100k-100c.txt -m batch -r 0.05 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=2)索引 ----
printf -- "-tm CVP22 -t vector -i vp -pn 2 -pbn 2 -f data/vector/clusteredvector-2d-100k-100c.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
echo 索引名: vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition2-maxLeafSize20
printf -- "-tm CVP22 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/clusteredvector-2d-100k-100c.txt -m batch -r 0.05 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=3)索引 ----
printf -- "-tm CVP23 -t vector -i vp -pn 2 -pbn 3 -f data/vector/clusteredvector-2d-100k-100c.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
echo 索引名: vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition3-maxLeafSize20
printf -- "-tm CVP23 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition3-maxLeafSize20 -qf data/vector/clusteredvector-2d-100k-100c.txt -m batch -r 0.05 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- AT索引 ----
printf -- "-tm CAT -t vector -i at -pn 2 -pbn 3 -f data/vector/clusteredvector-2d-100k-100c.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
echo 索引名: vector-LOCAL-5000-at-FFT-numPivots2-APOLLONIAN-numPartition3-maxLeafSize20
printf -- "-tm CAT -tbn vector-LOCAL-5000-at-FFT-numPivots2-APOLLONIAN-numPartition3-maxLeafSize20 -qf data/vector/clusteredvector-2d-100k-100c.txt -m batch -r 0.05 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ==== hawii数据集测试 ====
echo ---- GH索引 ----
printf -- "-tm HGH -t vector -i gh -f data/vector/hawii.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm HGH -tbn vector-LOCAL-5000-gh-FFT-GH-numPartitionnull-maxLeafSize20 -qf data/vector/hawii.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=1,pbn=2)索引 ----
printf -- "-tm HVP12 -t vector -i vp -pn 1 -pbn 2 -f data/vector/hawii.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm HVP12 -tbn vector-LOCAL-5000-vp-FFT-numPivots1-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/hawii.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=2)索引 ----
printf -- "-tm HVP22 -t vector -i vp -pn 2 -pbn 2 -f data/vector/hawii.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm HVP22 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/hawii.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=3)索引 ----
printf -- "-tm HVP23 -t vector -i vp -pn 2 -pbn 3 -f data/vector/hawii.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm HVP23 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition3-maxLeafSize20 -qf data/vector/hawii.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- AT索引 ----
printf -- "-tm HAT -t vector -i at -pn 2 -pbn 3 -f data/vector/hawii.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm HAT -tbn vector-LOCAL-5000-at-FFT-numPivots2-APOLLONIAN-numPartition3-maxLeafSize20 -qf data/vector/hawii.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ==== texas数据集测试 ====
echo ---- GH索引 ----
printf -- "-tm TGH -t vector -i gh -f data/vector/texas.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm TGH -tbn vector-LOCAL-5000-gh-FFT-GH-numPartitionnull-maxLeafSize20 -qf data/vector/texas.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=1,pbn=2)索引 ----
printf -- "-tm TVP12 -t vector -i vp -pn 1 -pbn 2 -f data/vector/texas.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm TVP12 -tbn vector-LOCAL-5000-vp-FFT-numPivots1-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/texas.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=2)索引 ----
printf -- "-tm TVP22 -t vector -i vp -pn 2 -pbn 2 -f data/vector/texas.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm TVP22 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/texas.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=3)索引 ----
printf -- "-tm TVP23 -t vector -i vp -pn 2 -pbn 3 -f data/vector/texas.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm TVP23 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition3-maxLeafSize20 -qf data/vector/texas.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- AT索引 ----
printf -- "-tm TAT -t vector -i at -pn 2 -pbn 3 -f data/vector/texas.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm TAT -tbn vector-LOCAL-5000-at-FFT-numPivots2-APOLLONIAN-numPartition3-maxLeafSize20 -qf data/vector/texas.txt -m batch -r 0.04 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ==== Uniform数据集测试 ====
echo ---- GH索引 ----
printf -- "-tm UGH -t vector -i gh -f data/vector/Uniform-20-d-vector.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm UGH -tbn vector-LOCAL-5000-gh-FFT-GH-numPartitionnull-maxLeafSize20 -qf data/vector/Uniform-20-d-vector.txt -m batch -r 0.1 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=1,pbn=2)索引 ----
printf -- "-tm UVP12 -t vector -i vp -pn 1 -pbn 2 -f data/vector/Uniform-20-d-vector.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm UVP12 -tbn vector-LOCAL-5000-vp-FFT-numPivots1-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/Uniform-20-d-vector.txt -m batch -r 0.1 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=2)索引 ----
printf -- "-tm UVP22 -t vector -i vp -pn 2 -pbn 2 -f data/vector/Uniform-20-d-vector.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm UVP22 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition2-maxLeafSize20 -qf data/vector/Uniform-20-d-vector.txt -m batch -r 0.1 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- VP(pn=2,pbn=3)索引 ----
printf -- "-tm UVP23 -t vector -i vp -pn 2 -pbn 3 -f data/vector/Uniform-20-d-vector.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm UVP23 -tbn vector-LOCAL-5000-vp-FFT-numPivots2-BALANCED-numPartition3-maxLeafSize20 -qf data/vector/Uniform-20-d-vector.txt -m batch -r 0.1 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo ---- AT索引 ----
printf -- "-tm UAT -t vector -i at -pn 2 -pbn 3 -f data/vector/Uniform-20-d-vector.txt -n 5000 -maxLeaf 20\n" | java -cp "target/classes;libs/*" app.Application -bi
printf -- "-tm UAT -tbn vector-LOCAL-5000-at-FFT-numPivots2-APOLLONIAN-numPartition3-maxLeafSize20 -qf data/vector/Uniform-20-d-vector.txt -m batch -r 0.1 -qn 5000\n" | java -cp "target/classes;libs/*" app.Application -qi
echo.

echo 测试完成！
pause