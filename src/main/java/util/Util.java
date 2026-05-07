package util;

import algorithms.datapartition.*;
import algorithms.pivotselection.PivotSelectionMethods;
import index.structure.VPPartitionResults;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 为项目提供一些工具方法
 */
public class Util
{
    public static String listEnumNames(Class cls)
    {
        String ret = "";
        String clsName = cls.getSimpleName();
        if (clsName.equalsIgnoreCase(VPPartitionMethods.class.getSimpleName())){
            ret = String.join(",",Arrays.stream(VPPartitionMethods.values()).map(Enum::name).collect(Collectors.toList()));
        }else if (clsName.equalsIgnoreCase(GHPartitionMethods.class.getSimpleName())){
            ret = String.join(",",Arrays.stream(GHPartitionMethods.values()).map(Enum::name).collect(Collectors.toList()));
        }else if (clsName.equalsIgnoreCase(PCTPartitionMethods.class.getSimpleName())){
            ret = String.join(",",Arrays.stream(PCTPartitionMethods.values()).map(Enum::name).collect(Collectors.toList()));
        }else if (clsName.equalsIgnoreCase(GNATPartitionMethods.class.getSimpleName())){
            ret = String.join(",",Arrays.stream(GNATPartitionMethods.values()).map(Enum::name).collect(Collectors.toList()));
        }else if (clsName.equalsIgnoreCase(CPPartitionMethods.class.getSimpleName())){
            ret = String.join(",",Arrays.stream(CPPartitionMethods.values()).map(Enum::name).collect(Collectors.toList()));
        }else if (clsName.equalsIgnoreCase(PivotSelectionMethods.class.getSimpleName())){
            ret = String.join(",",Arrays.stream(PivotSelectionMethods.values()).map(Enum::name).collect(Collectors.toList()));
        }
        return ret;
    }

    public static void main(String[] args)
    {
        System.out.println(listEnumNames(PivotSelectionMethods.class));
    }
}
