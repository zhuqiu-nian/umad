package db.type;

import java.io.Serializable;

/**
 * A compact representation of sequences of small alphabets whose indices fit within a valid
 * byte range. The general contract of {@link Sequence} implementations in this package is that all instances be immutable. This class has constructors that make
 * references to external objects in instances. <em>It is the responsibility
 * of the user to ensure that the external objects are immutable!</em>
 */
public abstract class Sequence implements Serializable
{
    private static final long   serialVersionUID = 6518919187067691379L;
    protected            byte[] data;
    String sequenceID;

    /**
     * 构造函数
     *
     * @param sequenceID 序列ID
     * @param sequence   序列对象
     */
    protected Sequence(String sequenceID, String sequence)
    {
        this.sequenceID = sequenceID;
        int stringLength = sequence.length();
        this.data = new byte[stringLength];
    }

    /**
     * get the size of the {@link Sequence}.
     *
     * @return the size of the {@link Sequence}.
     */
    public final int size()
    {
        return data.length;
    }

    /**
     * The number of fragments a given {@link Sequence} can be divided up into.
     *
     * @param fragmentLength the length of the fragments to split the {@link Sequence} into
     * @return the number of fragments for the given {@link Sequence}
     */
    public int numFragments(int fragmentLength)
    {
        return data.length - fragmentLength;
    }

    /**
     * get the {@link Symbol} corresponding to that index.
     *
     * @param index an integer index value
     * @return the {@link Symbol} corresponding to that index.
     */
    public abstract Symbol get(int index);


    public abstract String toString();


    public abstract Alphabet getAlphabet();
}
