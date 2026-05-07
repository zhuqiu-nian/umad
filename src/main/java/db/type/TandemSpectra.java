package db.type;

import db.table.Table;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;


/**
 * TandemSpectra represents a given spectra and its attached precursor mass.
 */
public class TandemSpectra extends Spectra
{
    private static final long serialVersionUID = 1247643270939539788L;

    private double precursorMass;

    /**
     * Necessary for readExternal() and writeExternal().
     */
    public TandemSpectra()
    {
    }

    /**
     * Constructs a TandemSpectra object from a representing
     * the tandem spectra. The only difference between a Spectra and a
     * TandemSpectra is that a TandemSpectra includes the precursor mass.
     *
     * @param table         the corresponding TandemSpectraTable for this object
     * @param rowID         the rowID in the TandemSpectraTable.
     * @param precursorMass the precursor mass of the spectra itself.
     * @param spectra       a space-seperated {@link String} representation of a single fragmentation spectra.
     */
    public TandemSpectra(Table table, int rowID, double precursorMass, String spectra)
    {
        super(table, rowID, spectra);
        this.precursorMass = precursorMass;
    }

    /**
     * Main constructor using an array of doubles to define the Spectra.
     *
     * @param table         the corresponding TandemSpectraTable for this object
     * @param rowID         the rowID in the TandemSpectraTable.
     * @param precursorMass the precursor mass of the spectra itself.
     * @param spectra       the spectra
     */
    public TandemSpectra(Table table, int rowID, double precursorMass, double[] spectra)
    {
        super(table, rowID, spectra);
        this.precursorMass = precursorMass;
    }

    /**
     * get the precursor mass for this tandem spectra.
     *
     * @return the precursor mass for this tandem spectra.
     */
    public double getPrecursorMass()
    {
        return precursorMass;
    }


    public IndexObject[] expand()
    {
        IndexObject[] dbO = new IndexObject[rowIDLength];
        for (int i = 0; i < rowIDLength; i++)
        {
            dbO[i] = new TandemSpectra(table, rowIDStart + i, precursorMass, data);
        }
        return dbO;
    }

    /* (non-Javadoc)
     * @see type.DoubleVector#compareTo(type.IndexObject)
     */
    public int compareTo(IndexObject oThat)
    {
        if (!(oThat instanceof TandemSpectra)) throw new Error("not compatible");
        TandemSpectra that = (TandemSpectra) oThat;
        if (this == that) return 0;

        if (this.precursorMass < that.precursorMass) return -1;
        if (this.precursorMass > that.precursorMass) return 1;
        else
        {
            if (this.size() < that.size()) return -1;
            else if (this.size() > that.size()) return 1;
            else
            {
                for (int i = 0; i < this.size(); i++)
                {
                    double double1 = data[i];
                    double double2 = that.data[i];
                    if (double1 < double2) return -1;
                    else if (double1 > double2) return 1;
                }
                return 0;
            }
        }
    }

    /* (non-Javadoc)
     * @see type.DoubleVector#equals(java.lang.Object)
     */
    public boolean equals(Object that)
    {
        if (!(that instanceof TandemSpectra)) return false;
        else
        {
            TandemSpectra sWPMass = (TandemSpectra) that;
            if (this.precursorMass != sWPMass.precursorMass) return false;
            return Arrays.equals(this.data, ((TandemSpectra) that).data);
        }
    }

    // taken from Joshua Bloch's Effective Java
    public int hashCode()
    {
        int  result = super.hashCode();
        long _long  = Double.doubleToLongBits(precursorMass);
        return 37 * result + (int) (_long ^ (_long >>> 32));
    }


    public String toString()
    {
        StringBuffer rowIDs = new StringBuffer("rowIDs: ");

        for (int i = 0; i < rowIDLength; i++)
        {
            rowIDs.append(table.getOriginalRowID(rowIDStart + i));
        }
        final int dataSize = data.length;
        rowIDs.append("data(size=" + dataSize + ", pMass= " + precursorMass + ") :[");
        for (int i = 0; i < dataSize; i++)
             rowIDs.append(data[i]).append(", ");
        rowIDs.append("]\n");
        return rowIDs.toString();
    }


    public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException
    {
        super.readExternal(in);
        precursorMass = in.readDouble();
    }


    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeDouble(precursorMass);
    }
}
