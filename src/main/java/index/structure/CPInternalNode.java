/**
 * mobios.index.CPInternalNode 2019.07.24
 * Title:        CPT索引树的中间结点
 * Description:  CPT索引树的中间结点
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright Information: 
 *
 * Change Log: 
 * 	从mobios.index.VPInternalNode.java修改而来，添加了对法向量的支持，删除了对GHTDegree的支持
 */
package index.structure;

import db.type.IndexObject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 完全线性划分树的内部节点
 * 新添了参数法向量组 normalVectorGroup
 * 每个扇出中的数据中存储划分出该扇出的法向量组 normalVectorsGroup
 * 新添了参数longestDistanceTopivots
 * 每个扇出的数据到该内部节点的每个支撑点的距离的最远值 longestDistanceToPivots  为了在范围查询时，可以知道该扇出到每个节点的最远距离，来判断是否可以利用包含关系，直接将该扇出的数据作为结果返回
 */
public class CPInternalNode extends InternalNode
{
    private static final long serialVersionUID = 2951629361525110954L;

    private double[][]        lowerRange;
    private double[][]        upperRange;
    
	private List<Vector<Double>> normalVectorGroup;       // 每个扇出中的数据中存储划分出该扇出的法向量组


    public List<Vector<Double>> getNormalVectorGroup()
    {
		return this.normalVectorGroup;
	}

	public void setNormalVectorGroup(List<Vector<Double>> normalVectorGroup)
	{
		this.normalVectorGroup = normalVectorGroup;
	}

    private double[][] longestDistanceTopivots;      // 每个扇出的数据到该内部节点的每个支撑点的距离的最远值

    public double[][] getLongestDistanceTopivots()
    {
        return this.longestDistanceTopivots;
    }

    public void setLongestDistanceTopivots(double[][] longestDistanceTopivots)
    {
        this.longestDistanceTopivots = longestDistanceTopivots;
    }

  	public CPInternalNode()
    {
        super();
    }

    /**
     * @param pivots
     * @param lowerRange
     *            lower range from each child (row) to each pivot (column)
     * @param upperRange
     *            upperrange from each child (row) to each pivot (column)
     * @param size
     * @param childAddress
     */
    public CPInternalNode(IndexObject[] pivots, double[][] lowerRange,
                          double[][] upperRange, int size, long[] childAddress)
    {
        this(pivots, lowerRange, upperRange, size, childAddress, 0);
    }

    /**
     *
     * @param pivots
     * @param lowerRange
     * @param upperRange
     * @param size
     * @param childAddresses
     * @param normalVectorGroup  对数据进行的划分的超平面的法向量集合
     * @param longestDistanceTopivots    vp截距
     */
    public CPInternalNode(IndexObject[] pivots, int size, long[] childAddresses, double[][] lowerRange, double[][] upperRange, List<Vector<Double>> normalVectorGroup, double[][] longestDistanceTopivots)
    {
        this(pivots, lowerRange, upperRange, size, childAddresses, 0);
        this.normalVectorGroup = normalVectorGroup;
        this.longestDistanceTopivots = longestDistanceTopivots;
    }

    
    private CPInternalNode(IndexObject[] pivots, double[][] lowerRange,
                           double[][] upperRange, int size, long[] childAddress, int degree)
    {
    	/*这个方法修改自VPInternalNode为方便起见，直接设置为private类型，不对外开放，即本类不再支持degree*/
        super(pivots, size, childAddress);

        if (lowerRange == null || upperRange == null)
            throw new IllegalArgumentException(
                    "lowerRange and upperRange distance arrays cannot be null");

        this.lowerRange = lowerRange;
        this.upperRange = upperRange;
    }

    /**
     * Returns the predicate, the ranges from the child to each piovt, of a
     * child node.
     * 
     * @param childIndex
     * @return a 2-d array of the lower ranges (first row) and the upper ranges
     *         (second row) of the child to each pivot.
     */
    public double[][] getChildPredicate(int childIndex)
    {
        double[][] result = new double[2][];
        result[0] = lowerRange[childIndex];
        result[1] = upperRange[childIndex];

        return result;
    }

    /**
     * 	向外存写入结点信息
     * */
    public void writeExternal(ObjectOutput out) throws IOException 
    {
        super.writeExternal(out);
        out.writeInt(childAddresses.length);
        for (int i = 0; i < childAddresses.length; i++) 
        {
            out.writeLong(childAddresses[i]);
        }
        /*对称地写入和读出法向量信息，创建CPInternalNode的时候再移过去*/
        int dim = 0;
        int numVector = normalVectorGroup.size();
        if (numVector > 0)
        	dim = normalVectorGroup.get(0).size();
        
        out.writeInt(numVector);   //写入向量个数
        out.writeInt(dim);		   //写入向量维度
        
        for (int i = 0; i < numVector; i++)
        {
        	Vector<Double> vector = normalVectorGroup.get(i);   //取出向量
        	for (int j = 0; j < dim; j++)				   //取出各个坐标写入外存
        	{
        		double coor = 0.0;
        		coor = vector.get(j).doubleValue();
        		out.writeDouble(coor);
        	}
        }

        /*保存vpIntecept*/
        out.writeInt(longestDistanceTopivots.length);
        for (int i = 0; i < longestDistanceTopivots.length; i++)
        {
            out.writeInt(longestDistanceTopivots[i].length);
            for (int j = 0; j < longestDistanceTopivots[i].length; j++)
            {
                out.writeDouble(longestDistanceTopivots[i][j]);
            }
        }
        
        /*和VPInternalNode一样，需要对范围信息进行保存*/
        out.writeInt(lowerRange.length);
        for (int i = 0; i < lowerRange.length; i++)
        {
            out.writeInt(lowerRange[i].length);
            for (int j = 0; j < lowerRange[i].length; j++)
            {
                out.writeDouble(lowerRange[i][j]);
            }
        }
        out.writeInt(upperRange.length);
        for (int i = 0; i < upperRange.length; i++)
        {
            out.writeInt(upperRange[i].length);
            for (int j = 0; j < upperRange[i].length; j++)
            {
                out.writeDouble(upperRange[i][j]);
            }
        }
    }
    
    /**
     * 	从外存中读出结点数据
     * */
    public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException 
    {
        super.readExternal(in);
        childAddresses = new long[in.readInt()];
        for (int i = 0; i < childAddresses.length; i++) 
        {
            childAddresses[i] = in.readLong();
        }
        
        /*对称地写入和读出法向量信息，创建CPInternalNode的时候再移过去*/
        int dim = 0;
        int numVector = 0;
        
        numVector = in.readInt();	//读入向量个数
        dim = in.readInt();			//读入向量维度
        this.normalVectorGroup = new ArrayList<Vector<Double>>();  //创建向量列表
        for (int i = 0; i < numVector; i++)
        {
        	Vector<Double> vector = new Vector<Double>();     //创建向量
        	for (int j = 0; j < dim; j++)					  //逐个坐标赋值
        	{
        		double coor = 0.0;
        		coor = in.readDouble();
        		vector.add(new Double(coor));
        	}
        	this.normalVectorGroup.add(vector);
        }

        /*保存vpIntecept*/
        longestDistanceTopivots = new double[in.readInt()][];
        for (int i = 0; i < longestDistanceTopivots.length; i++)
        {
            longestDistanceTopivots[i] = new double[in.readInt()];
            for (int j = 0; j < longestDistanceTopivots[i].length; j++)
            {
                longestDistanceTopivots[i][j] = in.readDouble();
            }
        }

        /*和VPInternalNode一样，需要对范围信息进行保存*/
        lowerRange = new double[in.readInt()][];
        for (int i = 0; i < lowerRange.length; i++)
        {
            lowerRange[i] = new double[in.readInt()];
            for (int j = 0; j < lowerRange[i].length; j++)
            {
                lowerRange[i][j] = in.readDouble();
            }
        }
        upperRange = new double[in.readInt()][];
        for (int i = 0; i < upperRange.length; i++)
        {
            upperRange[i] = new double[in.readInt()];
            for (int j = 0; j < upperRange[i].length; j++)
            {
                upperRange[i][j] = in.readDouble();
            }
        }
    }
}
