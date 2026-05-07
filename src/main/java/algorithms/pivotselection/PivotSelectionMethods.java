package algorithms.pivotselection;

import cern.colt.matrix.DoubleMatrix2D;
import db.type.IndexObject;
import metric.Metric;
import util.LargeDenseDoubleMatrix2D;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * 所有内置的支撑点选择算法.
 * <p>
 * FFT: 使用 Farthest-First-Traversal 选择数据的角落点。
 * RANDOM: 首先随机选择pivots，但不保证性能。
 * PCA: 使用PCA选取支撑点。
 * EPCAF: 使用快速PCA选取支撑点。
 *
 * @author Rui Mao, Willlard
 * @version 2006.08.03
 */
public enum PivotSelectionMethods implements PivotSelectionMethod
{
    /**
     * 使用 Farthest-First-Traversal 选择数据的角落点
     */
    FFT
            {
                /**
                 * @param metric 距离函数
                 * @param data 数据
                 * @param numPivots 支撑点数目
                 * @return 返回被选择的支撑点在原数据列表data中的下标
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
                {
                    return selectPivots(metric, data, 0, data.size(), numPivots);
                }

                /**
                 * 从数据列表data的部分数据中选择支撑点。
                 * @param metric 距离函数
                 * @param data 数据
                 * @param first 第一个元素的偏移量
                 * @param dataSize 数据大小
                 * @param numPivots 支撑点数目
                 * @return 返回被选择的支撑点在原数据列表data中的下标
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots)
                {
                    int firstPivot = first; // Math.floor(first + Math.random() *
                    // dataSize);
                    return selectPivots(metric, data, first, dataSize, numPivots, firstPivot);
                }

                /**
                 * 依据所提供的第一个支撑点，进一步完成整体的支撑点选择
                 * @param metric 距离函数
                 * @param data 数据集合
                 * @param first 第一个元素的偏移量
                 * @param dataSize 数据大小
                 * @param numPivots 支撑点数目
                 * @param firstPivot 第一个支撑点ID
                 * @return 返回被选择的支撑点在原数据列表data中的下标
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots, int firstPivot)
                {
                    if (numPivots >= dataSize)
                    {
                        int[] pivots = new int[dataSize];
                        for (int i = first; i < dataSize + first; i++)
                             pivots[i - first] = i;

                        return IncrementalSelection.removeDuplicate(metric, data, pivots);
                    }

                    boolean[] isCenter = new boolean[dataSize];
                    double[]  minDist  = new double[dataSize];
                    for (int i = 0; i < dataSize; i++)
                    {
                        isCenter[i] = false;
                        minDist[i]  = Double.POSITIVE_INFINITY;
                    }

                    isCenter[firstPivot] = true;

                    int[] indices = new int[numPivots]; // offsets of the pivots in the
                    // original data list

                    indices[0] = firstPivot;
                    for (int i = 1; i < indices.length; i++)
                         indices[i] = -1;
                    // array counter init to 1 since the first point is found already
                    for (int centerSize = 1; centerSize < indices.length; centerSize++)
                    {
                        double            currMaxDist = Double.NEGATIVE_INFINITY;
                        final IndexObject lastCenter  = data.get(indices[centerSize - 1]);

                        for (int i = 0; i < dataSize; i++)
                        {
                            if (isCenter[i] == false) // if point is not a center
                            {
                                double tempDist = metric.getDistance(data.get(i + first), lastCenter);

                                minDist[i] = (tempDist < minDist[i]) ? tempDist : minDist[i];

                                // TODO
                                if (minDist[i] > currMaxDist)
                                {
                                    indices[centerSize] = i; // save the index the
                                    // current farthest
                                    // point
                                    currMaxDist = minDist[i];
                                }

                            }
                        }

                        if (indices[centerSize] == -1) break;
                        else isCenter[indices[centerSize]] = true;
                    }
                    int returnSize = 0;
                    while ((returnSize < indices.length) && (indices[returnSize] >= 0)) returnSize++;

                    if (returnSize < indices.length)
                    {
                        int[] result = new int[returnSize];
                        System.arraycopy(indices, 0, result, 0, returnSize);
                        return result;
                    } else return indices;
                }
            },
    //    /**
    //     * 选择内部簇的中心，一种类似于CLARA的方法。(未实现）
    //     */
    //       CENTER
    //            {
    //                /**
    //                 * @param metric 距离函数
    //                 * @param data 数据
    //                 * @param numPivots 支撑点数目
    //                 * @return 分区结果对象 {@link index.structure.PartitionResults}
    //                 */
    //                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
    //                {
    //                    return selectPivots(metric, data, 0, data.size(), numPivots);
    //                }
    //
    //                //        /**
    ////         *
    ////         * @param metric 距离函数
    ////         * @param data 数据
    ////         * @param first 第一个支撑点ID
    ////         * @param dataSize 数据大小
    ////         * @param numPivots 选择的支撑点数目
    ////         * @return 分区结果对象 {@link PartitionResults}(未实现）
    ////         */
    //                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots)
    //                {
    //                    // TODO
    //                    return null;
    //                }
    //            },

    /**
     * 首先随机选择pivots，但不保证性能。
     */
    RANDOM
            {
                /**
                 *
                 * @param metric 距离函数
                 * @param data 数据
                 * @param numPivots 支撑点数目
                 * @return 分区结果对象 {@link index.structure.PartitionResults}
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
                {
                    return selectPivots(metric, data, 0, data.size(), numPivots);
                }

                /**
                 *
                 * @param metric 距离函数
                 * @param data 数据
                 * @param first 第一个元素的偏移量
                 * @param dataSize 数据大小
                 * @param numPivots 选择的支撑点数目
                 * @return 分区结果对象 {@link index.structure.PartitionResults}
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, final int numPivots)
                {
                    return selectPivots(metric, data, first, dataSize, numPivots, false);
                }

                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, final int first, int dataSize, final int numPivots, boolean debug)
                {
                    return randomPivot(metric, data.subList(first, first + dataSize), numPivots);
                }
                //
                //        /**
                //         * select pivots randomly. No duplicates are allowed in return.
                //         *
                //         * @param metric
                //         * @param data
                //         * @param numP
                //         *            number of pivots to select
                //         * @return an int array of subscripts of the pivots in the input data
                //         *         array.
                //         */

                /**
                 * 随机选择支撑点。不允许复制作为返回。
                 * @param metric 距离函数
                 * @param data 数据集合
                 * @param numP 支撑点数目
                 * @return 支撑点集合
                 */
                int[] randomPivot(Metric metric, List<? extends IndexObject> data, int numP)
                {
                    // final boolean debug = true;
                    final int LoopConstant = 5;
                    final int size         = data.size();

                    // if number of pivots to select is not smaller than the dataset
                    // size
                    // return all the not identical points
                    if (numP >= size)
                    {
                        int[] result  = new int[size];
                        int   counter = 1; // number of pivots selected
                        result[0] = 0;
                        for (int i = 1; i < size; i++)
                        {
                            if (!containsZeroDistance(metric, data, result, data.get(i), 0, counter))
                            {
                                result[counter] = i;
                                counter++;
                            }
                        }

                        if (counter == size) // no duplicate
                            return result;
                        else
                        {
                            int[] r = new int[counter];
                            System.arraycopy(result, 0, r, 0, counter);
                            return r;
                        }

                    }

                    // number of pivots is less than dataset size. linear scan to
                    // randomly choose pivots, be
                    // careful to the duplicate.
                    int    counter = 0; // number of pivots selected
                    int[]  result  = new int[numP];
                    Random r       = new Random();
                    for (int j = 0; j < LoopConstant; j++)
                    {
                        for (int i = 0; i < size; i++)
                        {
                            double d  = (double) (numP - counter) / size;
                            double nd = r.nextDouble();
                            // System.out.println("d =" + d + ", nd = " + nd);
                            if ((d > nd) && !containsZeroDistance(metric, data, result, data.get(i), 0, counter))
                            {
                                result[counter] = i;
                                counter++;
                                if (counter >= numP) break;
                            }
                        }

                        if (counter >= numP) break;
                    }

                    // if enough number of pivots are found, just return it.
                    if (counter >= numP) return result;

                    // otherwise, which means too much duplicate, scan it again.
                    int[] subscript = new int[size];
                    for (int i = 0; i < size; i++)
                         subscript[i] = i;

                    int remain = size; // number of points among which to select
                    // pivots, the duplicates
                    // that have already been identified are not included.

                    int temp = 0;
                    while ((counter < numP) && (remain > 0))
                    {
                        for (int i = 0; i < remain; i++)
                        {
                            if (r.nextDouble() < (double) (numP - counter) / remain)
                            {
                                if (containsZeroDistance(metric, data, result, data.get(subscript[i]), 0, counter))
                                {
                                    remain--;
                                    if (remain <= 0) break;

                                    temp              = subscript[i];
                                    subscript[i]      = subscript[remain];
                                    subscript[remain] = temp;
                                } else
                                {
                                    result[counter] = subscript[i];
                                    counter++;
                                    if (counter >= numP) break;
                                }
                            }
                        }
                        if (counter >= numP) break;
                    }

                    // if enough pivots are found, return it
                    if (counter >= numP) return result;

                    // otherwise, return all the pivots found
                    int[] rr = new int[counter];
                    System.arraycopy(result, 0, rr, 0, counter);
                    return rr;
                }

                /**
                 * @param metric 距离函数
                 * @param data 数据集合
                 * @param subscript 分区（？)
                 * @param probe 指针（？）
                 * @param first 第一个元素的偏移量
                 * @param last 最后一个元素的偏移量
                 * @return 是否包含
                 */
                boolean containsZeroDistance(Metric metric, List<? extends IndexObject> data, int[] subscript, IndexObject probe, int first, int last)
                {
                    if (data == null) return false;

                    boolean contains = false;

                    int i = first;
                    while ((i < last) && !contains)
                    {
                        if (metric.getDistance(data.get(subscript[i]), probe) == 0)
                        {
                            contains = true;
                            break;
                        } else i++;
                    }

                    return contains;
                }

            },

    /**
     * 使用PCA选取支撑点
     */
    PCA
            {
                /**
                 *
                 * @param metric 距离函数
                 * @param data 数据
                 * @param numPivots 支撑点数目
                 * @return 分区结果对象{@link PartitionResults}
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
                {
                    return selectPivots(metric, data, 0, data.size(), numPivots);
                }

                /**
                 *
                 * @param metric 距离函数
                 * @param data 数据
                 * @param first 第一个数据的偏移量
                 * @param dataSize 数据大小
                 * @param numPivots 选择的支撑点数目
                 * @return 分区结果对象{@link PartitionResults}
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots)
                {
                    //如果数据集大小小于等于支撑点个数，则数据集全部作为支撑点返回
                    if (numPivots >= dataSize){
                        int result[] = new int[dataSize];
                        for (int i=first; i<first+dataSize; i++) result[i-first] = i;
                        return result;
                    }
                    // compute the distance matrix
                    //计算距离对矩阵，matrix是n^2的实对称矩阵，存储了所有的距离对
                    DoubleMatrix2D matrix = algorithms.pivotselection.PCA.pairWiseDistance(metric, data);

                    // compute PCA with EM method
                    matrix = algorithms.pivotselection.PCA.EMPCA(matrix, numPivots);

                    // select pivots from the pca result
                    return algorithms.pivotselection.PCA.pivotSelectionByPCAResultAngle(matrix, numPivots);
                }
            },

    /**
     * 使用快速PCA选择支撑点
     */
    EPCAF
            {
                final int FFTScale = 10;

                final int NumPCScale = 2;

                final int NumPivotEachPC = 2;

                /**
                 *
                 * @param metric 距离函数
                 * @param data 数据
                 * @param first 第一个数据的偏移量
                 * @param dataSize 数据大小
                 * @param numPivots 选择的支撑点数目
                 * @return 分区结果对象{@link PartitionResults}
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots)
                {
                    int[] result = selectPivots(metric, data.subList(first, dataSize), numPivots);
                    for (int i = 0; i < result.length; i++)
                         result[i] += first;

                    return result;
                }

                /**
                 *
                 * @param metric 距离函数
                 * @param data 数据
                 * @param numPivots 支撑点数目
                 * @return 分区结果对象{@link PartitionResults}
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
                {
                    final int dataSize = data.size();

                    //如果数据集大小小于等于支撑点个数，则数据集全部作为支撑点返回
                    if (numPivots >= dataSize){
                        int result[] = new int[dataSize];
                        for (int i=0; i<dataSize; i++) result[i] = i;
                        return result;
                    }


                    // run fft to get a candidate set
                    int[] fftResult = FFT.selectPivots(metric, data, numPivots * FFTScale);
                    for (int i = 0; i < fftResult.length; i++)
                         System.out.print(fftResult[i] + "  ");
                    System.out.println();

                    // compute the distance matrix
                    if (fftResult.length <= Math.min(dataSize, numPivots)) return fftResult;

                    DoubleMatrix2D dataMatrix = LargeDenseDoubleMatrix2D.createDoubleMatrix2D(dataSize, fftResult.length);
                    for (int col = 0; col < fftResult.length; col++)
                        for (int row = 0; row < dataSize; row++)
                             dataMatrix.set(row, col, metric.getDistance(data.get(row), data.get(fftResult[col])));

                    // compute PCA with EM method, dataMatrix is centerized after the
                    // operation.
                    DoubleMatrix2D pcaResult = algorithms.pivotselection.PCA.EMPCA(dataMatrix, numPivots * NumPCScale);

                    // select pivots from the pca result
                    int[] result = algorithms.pivotselection.PCA.pivotSelectionByPCAResultProjection(dataMatrix, pcaResult, numPivots * NumPCScale, numPivots * NumPCScale * NumPivotEachPC);
                    for (int i = 0; i < result.length; i++)
                         System.out.print(result[i] + "  ");
                    System.out.println();

                    return result;
                }
            },

    /**
     * 使用PAM算法选择支撑点
     */
    PAM
            {
                /**
                 * @param metric 距离函数
                 * @param data 数据
                 * @param numPivots 支撑点数目
                 * @return
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
                {
                    return selectPivots(metric, data, 0, data.size(), numPivots);
                }

                  /**
                 *
                 * @param metric 距离函数
                 * @param data 数据
                 * @param first 第一个支撑点ID
                 * @param dataSize 数据大小
                 * @param numPivots 选择的支撑点数目
                 * @return
                 */
                public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots)
                {
                    //如果数据集大小小于等于支撑点个数，则数据集全部作为支撑点返回
                    if (numPivots >= dataSize){
                        int result[] = new int[dataSize];
                        for (int i=0; i<dataSize; i++) result[i] = i;
                        return result;
                    }
                    IndexObject[] indexObjects = data.toArray(new IndexObject[0]);
                    indexObjects = Arrays.copyOfRange(indexObjects, first, indexObjects.length);
                    IndexObject[] result = CLARAHelper.PAM(metric, indexObjects, numPivots);
                    int[] ret = new int[numPivots];
                    for (int i = 0; i < numPivots; i++)
                    {
                        ret[i] = data.indexOf(result[i]);
                    }
                    return ret;
                }
        },

    /**
     * 使用Clara选择支撑点
     */
    CLARA
        {
            /**
             * @param metric 距离函数
             * @param data 数据
             * @param numPivots 支撑点数目
             * @return
             */
            public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int numPivots)
            {
                return selectPivots(metric, data, 0, data.size(), numPivots);
            }

            /**
             *
             * @param metric 距离函数
             * @param data 数据
             * @param first 第一个支撑点ID
             * @param dataSize 数据大小
             * @param numPivots 选择的支撑点数目
             * @return
             */
            public int[] selectPivots(Metric metric, List<? extends IndexObject> data, int first, int dataSize, int numPivots)
            {
                //如果数据集大小小于等于支撑点个数，则数据集全部作为支撑点返回
                if (numPivots >= dataSize){
                    int result[] = new int[dataSize];
                    for (int i=0; i<dataSize; i++) result[i] = i;
                    return result;
                }

                IndexObject[] indexObjects = data.toArray(new IndexObject[0]);
                indexObjects = Arrays.copyOfRange(indexObjects, first, indexObjects.length);
                IndexObject[] result = CLARAHelper.selectCenter(metric, indexObjects, numPivots);
                int[] ret = new int[numPivots];
                for (int i = 0; i < numPivots; i++)
                {
                    ret[i] = data.indexOf(result[i]);
                }
                return ret;
            }
        }

}
