package db.type;

import db.TableManager;
import db.table.Table;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class StringObject extends IndexObject {
    private static final long serialVersionUID = 7835791430062789094L;

    private Table table;
    private String value;

    public StringObject() {
    }

    public StringObject(Table table, int rowID, String value) {
        super(rowID);
        if (value == null) {
            throw new IllegalArgumentException("String value cannot be null");
        }
        this.table = table;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int size() {
        return value.length();
    }

    @Override
    public IndexObject[] expand() {
        IndexObject[] expanded = new IndexObject[rowIDLength];
        for (int i = 0; i < rowIDLength; i++) {
            expanded[i] = new StringObject(table, rowIDStart + i, value);
        }
        return expanded;
    }

    @Override
    public int compareTo(IndexObject oThat) {
        if (!(oThat instanceof StringObject)) {
            throw new ClassCastException("not compatible");
        }
        return value.compareTo(((StringObject) oThat).value);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof StringObject)) {
            return false;
        }
        return value.equals(((StringObject) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(value);
        out.writeObject(table.getTableManagerName());
        out.writeInt(table.getTableLocation());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        value = in.readUTF();
        String tableManagerName = (String) in.readObject();
        table = TableManager.getTableManager(tableManagerName).getTable(in.readInt());
    }
}
