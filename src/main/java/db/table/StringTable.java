package db.table;

import app.Application;
import db.type.StringObject;
import metric.EditDistanceMetric;
import metric.Metric;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class StringTable extends Table {
    private static final long serialVersionUID = -2879618668302587995L;

    private static final Metric DEFAULT_METRIC = new EditDistanceMetric();

    public StringTable(String fileName, String indexPrefix, int size) throws IOException {
        this(fileName, indexPrefix, size, DEFAULT_METRIC);
    }

    public StringTable(String fileName, String indexPrefix, int size, Metric metric) throws IOException {
        super(fileName, indexPrefix, size, metric);
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        loadData(reader, size);
        if (data.size() < this.dataSize) {
            this.dataSize = data.size();
        }
    }

    @Override
    public void loadData(BufferedReader reader, int size) {
        ArrayList<StringObject> strings = new ArrayList<>();
        ArrayList<Integer> rowIDs = new ArrayList<>();
        int rowID = 0;

        try {
            String line = reader.readLine();
            while (line != null && strings.size() < size) {
                String value = line.trim();
                if (!value.isEmpty()) {
                    int dataRowID = strings.size();
                    rowIDs.add(rowID);
                    strings.add(new StringObject(this, dataRowID, value));
                }
                rowID++;
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error occurred when reading string file: " + e.getMessage(), e);
        }

        strings.trimToSize();
        data = strings;
        Application.globalData = strings;
        originalRowIDs = new int[rowIDs.size()];
        for (int i = 0; i < rowIDs.size(); i++) {
            originalRowIDs[i] = rowIDs.get(i);
        }
    }
}
