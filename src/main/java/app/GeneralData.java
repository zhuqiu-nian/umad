package app;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class GeneralData
{
    public static double[][] read(String fileName) throws IOException {


        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line = br.readLine();
        int dim = Integer.parseInt(line.split(" ")[0]);
        int size = Integer.parseInt(line.split(" ")[1]);

        double[][] data = new double[size][dim];
        int count = 0;
        while ((line = br.readLine()) != null)
        {
            String[] tem = line.split(" ");
            for (int i = 0; i < data[count].length; i++)
            {
                 data[count][i] = Double.parseDouble(tem[i]);
            }

            count++;
        }
        
        return data;
    }
}
