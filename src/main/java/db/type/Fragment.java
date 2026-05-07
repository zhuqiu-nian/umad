package db.type;

import db.TableManager;
import db.table.SequenceTable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * 代表序列片段
 *
 * @author Willard
 */
public class Fragment extends IndexObject
{

    private static final long serialVersionUID = -7087849259510041868L;


    private SequenceTable sTable;


    public Fragment()
    {
    }

    /**
     * @param table 绑定的序列数据表
     * @param rowID 行ID
     */
    public Fragment(SequenceTable table, int rowID)
    {
        super(rowID);
        this.sTable = table;
    }

    /**
     * @param i 第i个位置上的{@link Symbol}
     * @return 返回第第i个位置上的 {@link Symbol}
     */
    public Symbol get(int i)
    {
        return sTable.alphabet.get(sTable.sequences[sTable.originalRowIDs[rowIDStart]].data[sTable.fragmentOffsets[rowIDStart] + i]);
    }


    /**
     * @return 返回片段大小
     */
    public int size()
    {
        return sTable.fragmentLength;
    }


    public IndexObject[] expand()
    {
        IndexObject[] dbO = new IndexObject[rowIDLength];
        for (int i = 0; i < rowIDLength; i++)
        {
            dbO[i] = new Fragment(sTable, rowIDStart + i);
        }
        return dbO;
    }


    public int compareTo(IndexObject oThat)
    {
        if (!(oThat instanceof Fragment)) throw new Error("not compatible");
        Fragment that = (Fragment) oThat;
        if (this == that) return 0;

        if (this.size() < that.size()) return -1;
        else if (this.size() > that.size()) return 1;
        else
        {
            for (int i = 0; i < sTable.fragmentLength; i++)
            {
                byte byte1 = sTable.sequences[sTable.originalRowIDs[rowIDStart]].data[sTable.fragmentOffsets[rowIDStart] + i];
                byte byte2 = that.sTable.sequences[that.sTable.originalRowIDs[that.rowIDStart]].data[that.sTable.fragmentOffsets[that.rowIDStart] + i];
                if (byte1 < byte2) return -1;
                else if (byte1 > byte2) return 1;
            }
            return 0;
        }

    }


    public final boolean equals(Object object)
    {
        if (object == null) return false;

        if (!(object instanceof Fragment)) return false;

        Fragment that = (Fragment) object;

        if (this.size() != that.size()) return false;

        if (!this.sTable.alphabet.equals(that.sTable.alphabet)) return false;

        for (int i = 0; i < size(); ++i)
            if (this.get(i) != that.get(i)) return false;
        return true;
    }


    public int hashCode()
    {
        int result = super.hashCode();
        result = 37 * result + size();
        result = 37 * result + sTable.alphabet.hashCode();
        for (int i = 0; i < size(); ++i)
        {
            result = 37 * result + get(i).hashCode();
        }
        return result;
    }


    public String toString()
    {
        StringBuffer sourceSequence = new StringBuffer(rowIDLength);
        for (int i = 0; i < rowIDLength; i++)
        {
            sourceSequence.append(sTable.sequences[sTable.getOriginalRowID(rowIDStart + i)].sequenceID).append(", offset: ").append(sTable.getFragmentOffset(rowIDStart + i)).append(" ");
        }
        StringBuffer fragment = new StringBuffer(sTable.fragmentLength);
        for (int i = 0; i < sTable.fragmentLength; i++)
        {
            fragment.append(get(i));
        }
        return "fragment: " + fragment.toString() + " source: " + sourceSequence.toString();
    }


    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        String indexPrefix = (String) in.readObject();
        sTable = (SequenceTable) TableManager.getTableManager(indexPrefix).getTable(in.readInt());
    }


    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeObject(sTable.getTableManagerName());
        out.writeInt(sTable.getTableLocation());
    }
}
