package app;

import algorithms.datapartition.VPPartitionMethods;
import algorithms.pivotselection.PivotSelectionMethod;
import db.TableManager;
import db.table.*;
import index.type.HierarchicalPivotSelectionMode;
import util.Embed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 主要是提供一些度量空间分析工具
 */
public class UMADTool
{
    public static String mappingToPivotSpace(TableManager tableManager, String dataType, String dataFileName,int dim , int maxDataSize,
                                           PivotSelectionMethod pivotSelectionMethod,
                                           int numPivots) throws IOException
    {
        Table dataTable = null;
        String indexPrefix = "mappingDataTable";
        if (dataType.equalsIgnoreCase("ms"))
        {
            dataTable = new SpectraTable(dataFileName, indexPrefix, maxDataSize, null);
        }
        else if (dataType.equalsIgnoreCase("msms"))
        {
            dataTable = new SpectraWithPrecursorMassTable(dataFileName, indexPrefix, maxDataSize);
        }
        else if (dataType.equalsIgnoreCase("dna"))
        {
            dataTable = new DNATable(dataFileName, indexPrefix, maxDataSize, dim);
        }
        else if (dataType.equalsIgnoreCase("rna"))
        {
            dataTable = new RNATable(dataFileName, indexPrefix, maxDataSize, dim);
        }
        else if (dataType.equalsIgnoreCase("protein"))
        {
            dataTable = new PeptideTable(dataFileName, indexPrefix, maxDataSize, dim);
        }
        else if (dataType.equalsIgnoreCase("vector"))
        {
            dataTable = new DoubleVectorTable(dataFileName, indexPrefix, maxDataSize, dim);
        }
        else if (dataType.equalsIgnoreCase("image"))
        {
            dataTable = new ImageTable(dataFileName, indexPrefix, maxDataSize);
        }
        else
        {
            throw new Error("Invalid dataType!");
        }
        int[] pivots = pivotSelectionMethod.selectPivots(dataTable.getMetric(), dataTable.getData(), numPivots);
        Embed embed = new Embed();
        embed.embedToPivotSpace(dataTable, pivots);
        File  file  = new File(dataFileName);
        String fileName           = file.getName();
        String outputFileName = fileName.substring(0, fileName.lastIndexOf(".")) + "_mapped.txt";
        String outputFilePath = Paths.get(file.getAbsolutePath()).getParent().toString();
        String outputFile     = outputFilePath + "\\" + outputFileName;
        embed.writeToTxt(outputFile);
        return outputFile;
    }
}
