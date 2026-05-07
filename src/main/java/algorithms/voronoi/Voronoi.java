package algorithms.voronoi;

import de.alsclo.voronoi.graph.Edge;
import de.alsclo.voronoi.graph.Point;
import de.alsclo.voronoi.graph.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用于根据站点集合生成维诺图
 */
public class Voronoi extends de.alsclo.voronoi.Voronoi
{
    private ArrayList<Point> pointsList;

    private Voronoi(ArrayList<Point> points)
    {
        super(points);
        pointsList = points;
    }

    /**
     * 根据传入的站点数组构建一个维诺图
     * @param pointsArray 站点构成的数组
     * @return
     */
    public static Voronoi getAVoronoi(double[][] pointsArray){
        int n = pointsArray.length;
        ArrayList<Point> list = new ArrayList<>();
        for (int i = 0; i < n; i++)
        {
            list.add(new Point(pointsArray[i][0], pointsArray[i][1]));
        }
        return new Voronoi(list);
    }

    /**
     * 获取维诺图中的所有维诺边
     * @return
     */
    public List<VoronoiEdge> getVoronoiEdge(){
        List<Edge> edges = this.getGraph().edgeStream().collect(Collectors.toList());
        List<VoronoiEdge> voronoiEdges = new ArrayList<>();
        for (Edge e : edges)
        {
            Vertex a = e.getA();
            Vertex b = e.getB();
            Point  site1 = e.getSite1();
            Point  site2 = e.getSite2();
            if (b==null){
                b = calVertex(site1, site2);
                voronoiEdges.add(new VoronoiEdge(a, b, true, pointsList.indexOf(site1), pointsList.indexOf(site2)));
            }else
            {
                voronoiEdges.add(new VoronoiEdge(a, b, false, pointsList.indexOf(site1), pointsList.indexOf(site2)));
            }
        }
        return voronoiEdges;
    }

    private Vertex calVertex(Point site1, Point site2)
    {
        double x = (site1.x + site2.x)/2;
        double y = (site1.y + site2.y)/2;
        return new Vertex(new Point(x, y));
    }

    public static void main(String[] args)
    {
        double[][] arr = new double[][]{{1.0,1.0},{-3.8,1.3},{4.0,9.0}};
        Voronoi    aVoronoi = getAVoronoi(arr);
        List<VoronoiEdge> voronoiEdge = aVoronoi.getVoronoiEdge();
        voronoiEdge.forEach(System.out::println);
        System.out.println();

    }
}
