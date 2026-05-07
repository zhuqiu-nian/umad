package algorithms.voronoi;

import de.alsclo.voronoi.graph.Point;
import de.alsclo.voronoi.graph.Vertex;

import java.util.Arrays;

/**
 * 存储维诺边，同时存储维诺边所关联的两个站点的下标
 */
public class VoronoiEdge
{
    //point是站点 vertex是维诺图的顶点
    private int[] pointsIndex = new int[2];
    private double[][] vertexes = new double[2][2];  //a点是vertexes[0]  b点是vertexes[1]
    /**
     * 表示该边是否沿ab方向无限延伸
     */
    private boolean isInf;
    public VoronoiEdge(Vertex vertex1, Vertex vertex2, boolean isInf, int point1Index, int point2Index)
    {
        this.isInf = isInf;
        pointsIndex[0] = point1Index;
        pointsIndex[1] = point2Index;
        vertexes[0][0] = vertex1.getLocation().x;
        vertexes[0][1] = vertex1.getLocation().y;
        vertexes[1][0] = vertex2.getLocation().x;
        vertexes[1][1] = vertex2.getLocation().y;
    }

    public double[][] getEdge(){
        return vertexes;
    }

    public int[] getPointsIndex(){
        return pointsIndex;
    }

    public boolean getIsInf(){
        return isInf;
    }

    @Override
    public int hashCode()
    {
        return Integer.valueOf("" + pointsIndex[0] + pointsIndex[1]);
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj) return true;
        else if(obj!=null || this.getClass() == obj.getClass()){
            //如果两个边内存的站点是相同的，那么就认为这两条边是同一条边
            VoronoiEdge obj1 = (VoronoiEdge) obj;
            int[]       pointsIndex = obj1.getPointsIndex();
            if(pointsIndex[0]==this.pointsIndex[0]&&pointsIndex[1]==this.pointsIndex[1]||
                    (pointsIndex[1]==this.pointsIndex[0]&&pointsIndex[0]==this.pointsIndex[1])){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return String.format("VoronoiEdge{{(%1$.2f,%2$.2f),(%3$.2f,%4$.2f),%5$b},pointsIndex(%6$d, %7$d)}",
                vertexes[0][0],vertexes[0][1],vertexes[1][0],vertexes[1][1],
                isInf,pointsIndex[0],pointsIndex[1]);
    }
}
