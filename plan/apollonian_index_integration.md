# Apollonian Tree 索引接入 UMAD 系统完整记录

## 一、背景与目标

将 Apollonian Tree (AT) 度量空间索引从 C++ 移植到 UMAD Java 度量空间数据管理系统，使其能够：
1. 通过命令行 `-i at` 参数指定使用 AT 索引
2. 与系统中原有的 GNAT、VP-Index 等索引类型平级
3. 支持范围查询（RangeQuery）
4. 自动选择合适的度量函数和分区方法

## 二、AT 索引核心原理

Apollonian Tree 是一种基于**比率**的三分索引结构：

### 2.1 核心思想
- 使用**两个 pivot** (c1, c2) 对数据进行三分
- 根据 `ratio = d(x, c1) / d(x, c2)` 的比值划分数据：
  - **Left 分支**: ratio < c1_ratio
  - **Mid 分支**: c1_ratio <= ratio <= c2_ratio
  - **Right 分支**: ratio > c2_ratio
- 每个分支存储到 pivot 的最大距离 M1, M2 用于剪枝

### 2.2 剪枝策略
通过三���不等式判断分支是否需要搜索：
- Left 剪枝: `d(q,c1) - r > c1_ratio * (d(q,c2) + r)`
- Right 剪枝: `d(q,c2) - r > d(q,c1)/c2_ratio + r`
- Mid 分支一般需要搜索

## 三、新建文件（5个）

### 3.1 索引结构类

| 文件 | 路径 | 说明 |
|------|------|------|
| ApollonianIndex.java | `src/main/java/index/structure/` | 索引主类，继承 AbstractIndex，固定 numPivot=2, numPartitions=3 |
| ApollonianInternalNode.java | `src/main/java/index/structure/` | 内部节点类，存储 c1, c2, c1_ratio, c2_ratio, M1, M2 |
| ApollonianPartitionResults.java | `src/main/java/index/structure/` | 三分划分结果类 |

### 3.2 搜索光标类

| 文件 | 路径 | 说明 |
|------|------|------|
| ApollonianRangeCursor.java | `src/main/java/index/search/` | 范围搜索光标，实现三分剪枝逻辑 |

### 3.3 分区方法类

| 文件 | 路径 | 说明 |
|------|------|------|
| ApollonianPartitionMethods.java | `src/main/java/algorithms/datapartition/` | 分区方法枚举，实现 APOLLONIAN 分区逻辑 |

## 四、修改文件（3个）

### 4.1 Table.java (`src/main/java/db/table/Table.java`)
```java
// 添加方法
public void buildApollonianIndex(List<? extends IndexObject> objects, ApollonianIndex index,
    String metricName, int maxLeafSize, int bufferSize, PivotSelectionMethod pivotSelectionMethod)
```

### 4.2 IndexBuilder.java (`src/main/java/app/IndexBuilder.java`)
```java
// 添加方法（约7个）
public static void bulkLoadApollonianIndex(...) // 批量加载入口
public static void buildApollonianIndexOnVector(...) // 针对 vector 数据类型
public static void buildApollonianIndexOnDNA(...)
public static void buildApollonianIndexOnRNA(...)
public static void buildApollonianIndexOnProtein(...)
public static void buildApollonianIndexOnMS(...)
public static void buildApollonianIndexOnMsMs(...)
public static void buildApollonianIndexOnImage(...)
```

### 4.3 Application.java (`src/main/java/app/Application.java`)

**parseBuildIndexArgs()** 方法中添加：
```java
case "at":
    this.indexType = IndexType.AT;
    // AT 固定参数
    this.numPivot = 2;
    this.numPartitions = 3;
    // 默认分区方法
    if (this.partitionMethod == null) {
        this.partitionMethod = ApollonianPartitionMethods.APOLLONIAN;
    }
    break;
```

**executeIndexBuilder()** 方法中添加：
```java
case AT:
    // 调用 IndexBuilder 的 buildApollonianIndexOnXXX 方法
```

**promptForBuildIndex()** 方法中更新帮助信息，添加 at 索引类型说明。

## 五、接口设计要点

### 5.1 继承结构
```
AbstractIndex
    └── ApollonianIndex
        - pivotSelection() 返回 2 个 pivot
        - partition() 调用 ApollonianPartitionMethods.APOLLONIAN
        - search() 返回 ApollonianRangeCursor
```

### 5.2 分区方法枚举
```
PartitionMethod (接口)
    └── ApollonianPartitionMethods (枚举)
            └── APOLLONIAN
        - partition(metric, pivotSet, data, numPartitions, maxLeafSize)
          返回 ApollonianPartitionResults
```

### 5.3 搜索光标继承
```
RangeCursor
    └── ApollonianRangeCursor
        - willTheSubTreeFurtherSearch() 实现三分剪枝逻辑
```

## 六、关键代码片段

### 6.1 创建 AT 索引
```java
ApollonianIndex index = new ApollonianIndex(
    indexPrefix,
    dataList,
    metric,
    maxLeafSize,
    2,  // AT 固定为 2
    3,  // AT 固定为 3（三分）
    PivotSelectionMethods.FFT,
    ApollonianPartitionMethods.APOLLONIAN
);
index.buildTree();
```

### 6.2 命令行使用
```bash
java app.Application -bi -tm xxx -f data/xxx.txt -n 1000 -d 2 -i at -t vector
```

参数说明：
- `-i at`: 指定使用 Apollonian Tree 索引
- `-d 2`: 指定使用 2 个 pivot（AT 必须为 2）
- 分区方法默认使用 APOLLONIAN

## 七、遇到的问题及解决

### 7.1 类型不匹配
**问题**: `IndexBuilder` 中使用了 `PartitionMethod` 而不是 `ApollonianPartitionMethods`
**解决**: 修改参数类型为正确的枚举类型

### 7.2 中文引号编码问题
**问题**: Java 字符串中的中文引号 `""` 导致编译错误
**解决**: 使用转义字符 `\"` 或替换为英文引号

## 八、验证方式

1. **编译测试**
   ```bash
   mvn compile
   ```

2. **构建索引测试**
   ```bash
   java app.Application -bi -tm vector -f data/vector_1000.txt -n 1000 -d 2 -i at -t vector
   ```

3. **范围查询测试**
   ```bash
   java app.Application -sq -i at -f xxx.000 -q query.txt -r 0.05
   ```

## 九、未来扩展

### 9.1 添加更多数据类型支持
如果需要支持新的数据类型（如图像、时序数据等），需要：
1. 在 `IndexBuilder.java` 中添加对应的 `buildApollonianIndexOnXXX()` 方法
2. 在 `Application.java` 的 `executeIndexBuilder()` 中添加对应的 case 分支

### 9.2 优化剪枝策略
可以参考 C++ 版本的剪枝逻辑，优化 `ApollonianRangeCursor.willTheSubTreeFurtherSearch()` 方法中的剪枝判断条件。

### 9.3 添加 KNN 查询支持
当前仅支持范围查询，如需支持 KNN 查询：
1. 实现 `ApollonianKNNCursor` 类
2. 在 `ApollonianIndex.search()` 中添加对 `KNNQuery` 的处理

## 十、总结

将新索引接入 UMAD 系统的核心步骤：
1. **实现核心类**：继承 AbstractIndex、实现分区方法枚举、实现搜索光标
2. **修改 Table.java**：添加索引构建方法
3. **修改 IndexBuilder.java**：添加各数据类型的构建入口
4. **修改 Application.java**：添加命令行参数解析和执行分支
5. **测试验证**：编译并运行测试

关键点：
- 确定索引的固定参数（如 AT 的 numPivot=2, numPartitions=3）
- 遵循系统的继承结构和接口设计
- 在命令行参数解析中添加新的索引类型 case
- 为每种数据类型添加对应的构建方法