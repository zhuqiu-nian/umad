/*
@liulinfeng 2021/3/30 增加tableManger的toString方法和输出所有table方法
@liulinfeng,huayong 2021/4/3  实现GH树的建树操作,增加TableManger的自动关闭功能，支持try-with-resource
 */
package db;

import db.table.Table;
import manager.MckoiObjectIOManager;
import manager.ObjectIOManager;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Table管理器
 *
 * <p>
 * 该对象实例存储到磁盘上的时候，文件后缀是“-db"。整个项目只持有唯一的Table管理器对象。一个Table管理器对应多个Table。
 * </p>
 */
public class TableManager implements Serializable, AutoCloseable
{

    private static final long serialVersionUID = -6911571514920379964L;

    private static      TableManager               tableManager;
    /*<位置，table>*/
    transient private   Hashtable<Integer, Table>  tableHashtable;
    /*<前缀，位置>*/
    private final       Hashtable<String, Integer> tableAddress;
    /*<位置，虚存指针>*/
    private final       Hashtable<Integer, Long>   oiomLocations;
    /**
     * 存储到内存上的数据库文件的前缀
     * <p>
     * tableManager存储到内存中的文件名是“indexPrefix-db";
     * 其中的table存储到内存中的文件是“indexPrefix-umad.000"
     */
    private final       String                     indexPrefix;
    transient protected ObjectIOManager            oiom;
    private             int                        count;

    private TableManager(String prefix)
    {
        tableAddress     = new Hashtable<String, Integer>();
        oiomLocations    = new Hashtable<Integer, Long>();
        tableHashtable   = new Hashtable<Integer, Table>();
        this.indexPrefix = prefix;
    }


    /**
     * 根据前缀获取一个对应前缀的TableManager，并打开
     *
     * @param prefix 要获取的TableManager对应的前缀,该前缀控制tableManager序列化到内存的时候的序列化名称，
     *               同时，该前缀也作为这个tableManager中存储的table序列化到内存上的序列化名称。
     *               <p>
     *               tableManager存储到内存中的文件名是“indexPrefix-db";
     *               <p>
     *               table存储到内存中的文件是“indexPrefix-umad.000"
     * @return TableManager实例
     */
    public static TableManager getTableManager(String prefix)
    {
        if (tableManager == null)
        {
            //如果对应前缀的管理器已经在内存中存在，则读入;否则则创建一个新的对应前缀的管理器并返回
            if (new File(prefix + "-db").exists())
            {
                ObjectInputStream objectStream;
                try
                {
                    objectStream = new ObjectInputStream(new FileInputStream(prefix + "-db"));
                    tableManager = (TableManager) objectStream.readObject();
                } catch (IOException e)
                {
                    e.printStackTrace();
                } catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            } else
            {
                tableManager = new TableManager(prefix);
            }
            tableManager.open();
        }
        return tableManager;
    }

    /**
     * 打开 TableManager
     */
    protected void open()
    {
        initOIOM(false);
        openOIOM();
    }

    protected void initOIOM(boolean readOnly)
    {
        oiom = new MckoiObjectIOManager(this.indexPrefix + "-umad", "000", 1024 * 1024 * 1024, "Java IO", 4, 128 * 1024, readOnly);
    }

    protected void openOIOM()
    {
        try
        {

            if (!oiom.open())
            {
                throw new Error("Cannot open store for TableManager" + this.indexPrefix + "-umad.000");
            }
            // System.out.println("OIOM.size = " + oiom.size() + "\n");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 根据索引名，获取对应索引名的Table
     *
     * @param indexName 要查找的Table的索引名
     * @return Table实例，未找到则返回NULL
     */
    public Table getTable(String indexName)
    {
        if (!tableAddress.containsKey(indexName))
        {
            throw new RuntimeException("table索引表中不存在名称为：" + indexName + "的表！");
        }
        //根据索引名找到table在tableManager中的位置
        Integer tableLocation = tableAddress.get(indexName);
        if (tableHashtable.containsKey(tableLocation))
        {
            //内存中有该table，直接返回
            return tableHashtable.get(tableLocation);
        }
        //内存中不存在table，从磁盘中去拿
        Table  table = null;
        double startTime, endTime, runTime;
        System.out.println("The table already exists. No new table is created.");
        System.out.println("Deserializing table...");
        startTime = System.currentTimeMillis();
        try
        {
            //从缓存中取出一个table
            table = (Table) oiom.readObject(oiomLocations.get(tableLocation));
            //将拿到的table放入内存中
            tableHashtable.put(tableLocation, table);
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        } catch (InstantiationException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        endTime = System.currentTimeMillis();
        runTime = (endTime - startTime) / 1000.00;
        System.out.println("Time to deserialize table: " + runTime);
        return table;
    }

    public String getManagerName(){
        return indexPrefix;
    }

    /**
     * 根据tableIndex，从该TableManager中获取对应的Table
     *
     * @param tableIndex 要查询的TableIndex
     * @return 返回Table对象
     */
    public Table getTable(int tableIndex)
    {
        /*指定位置的table不在，在虚存中找*/
        if (!tableHashtable.containsKey(tableIndex))
        {
            try
            {
                tableHashtable.put(tableIndex, (Table) oiom.readObject(oiomLocations.get(tableIndex)));
            } catch (IOException e)
            {
                e.printStackTrace();
            } catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            } catch (InstantiationException e)
            {
                e.printStackTrace();
            } catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        return tableHashtable.get(tableIndex);
    }

    /**
     * 获取TableManager中的下一个可以存储的位置
     *
     * @return 返回TableManager中的下一个可以存储的位置
     */
    private synchronized int getNextLocation()
    {
        count++;
        return count;
    }

    /**
     * @return 返回TableManager管理的Table个数
     */
    public long size()
    {
        try
        {
            return oiom.size();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 将指定Table加入到TableManger的指定位置
     *
     * @param table         待加入的Table

     */
    public void putTable(Table table)
    {
        //设置table的location和名称
        int tableLocation = getNextLocation();
        table.setTableLocation(tableLocation);
        table.setTableManagerName(this.indexPrefix);
        tableHashtable.put(tableLocation, table);
        tableAddress.put(table.getTableIndexPrefix(), tableLocation);
    }

    /**
     * 在命令行窗口中输出该TableManager管理的所有的table表。
     */
    public void showAllTables()
    {
        System.out.println(this);
    }

    /**
     * 删除指定table表
     * @param tableName 要删除的table表的名字
     */
    public void deleteTable(String tableName) {
        if (!tableAddress.containsKey(tableName)){
            System.out.println("manager中不存在该名称的table表！");
            return;
        }
        int location = tableAddress.get(tableName);


        if (!tableHashtable.containsKey(location)){
            //table表还未加载入内存,加载入内存并删除index
            getTable(tableName).getIndex().destroy();
        } else {
            //table表在内存中，则直接删除index
            tableHashtable.get(location).getIndex().destroy();
        }

        //删除这个table的一切信息
        tableAddress.remove(tableName);
        oiomLocations.remove(location);
        tableHashtable.remove(location);

    }

    /**
     * toString 方法
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString()
    {
        StringBuilder tostring = new StringBuilder("============" + this.indexPrefix + "==========\n");
        tostring.append("location\tindexPrefix\n");
        Enumeration enumeration = tableAddress.keys();
        while (enumeration.hasMoreElements())
        {
            String prefix   = (String) enumeration.nextElement();
            int    location = tableAddress.get(prefix);
            tostring.append(location + "\t\t\t" + prefix + "\n");
        }
        tostring.append("=================================");
        return tostring.toString();
    }

    /**
     * 将TableManger写入硬盘，并关闭所有流
     */
    @Override
    public void close()
    {
        try
        {
            //将table写到磁盘上
            Enumeration<Integer> enumeration = tableHashtable.keys();
            long                 point;
            int                  i;
            while (enumeration.hasMoreElements())
            {
                i     = enumeration.nextElement();
                point = oiom.writeObject(tableHashtable.get(i));
                oiomLocations.put(i, point);
            }
            //将manager写到内存上
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(this.indexPrefix + "-db"));
            out.writeObject(tableManager);
            //关闭所有的读写流
            out.flush();
            out.close();
            oiom.close();
            tableManager = null;
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        tableHashtable = new Hashtable<Integer, Table>();
    }
}
