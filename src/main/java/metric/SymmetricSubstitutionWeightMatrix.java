package metric;


///**
// * Implements a {@link WeightMatrix} by providing a symmetric substitution matrix of double
// * distances for a given {@link Alphabet}. Thus, this defines a weighted edit distance on an
// * {@link Alphabet}
// *
// * @author Rui Mao, Willard
// * @version 2004.03.02
// */

import db.type.Alphabet;
import db.type.Symbol;

/**
 * 通过给定关于 {@link Alphabet}的对称替换矩阵实现 {@link WeightMatrix}。它定义了一个在 {@link Alphabet} 上的加权
 * 编辑距离。
 */
public class SymmetricSubstitutionWeightMatrix implements WeightMatrix
{
    /**
     *
     */
    private static final long serialVersionUID = 6295557519158168768L;

    private final Alphabet   alphabet;
    private final int        alphabetSize;
    private final double[][] distances;

    //    /**
    //     * This constructor provides the {@link Alphabet} and an array of double distances between the
    //     * {@link Symbol} objects. The distance array represents the lower triangle partof a symmetric
    //     * matrix of distances, The distance between two {@link Symbol} objects with indices <em>I</em>
    //     * and <em>J</em> will be computed by first computing <em>R=max(I,J)</em> and
    //     * <em> C=min(I,J) </em>, then indexing the distance array with index <em>R*(R+1)/2+C</em>.
    //     */

    /**
     * @param alphabet  {@link Symbol}集合 {@link Alphabet}
     * @param distances 定义在 {@link Alphabet}上的距离矩阵。
     */
    public SymmetricSubstitutionWeightMatrix(Alphabet alphabet, double[][] distances)
    {
        this.alphabetSize = alphabet.distinctSize();
        int minLength = alphabetSize;
        if (minLength > distances.length)
            throw new IndexOutOfBoundsException("array \"distances\" is length " + distances.length + "; too small for \"alphabet\" of size " + alphabetSize);
        for (int i = 0; i < distances.length; i++)
        {
            if (minLength > distances[i].length)
            {
                throw new IndexOutOfBoundsException("array \"distances\" is length " + distances.length + "; too small for \"alphabet\" of size " + alphabetSize);
            }
        }

        this.alphabet  = alphabet;
        this.distances = distances;
    }

    /*
     * (non-Javadoc)
     *
     * @see mobios.metric.WeightMatrix#getDistance(mobios.type.Symbol, mobios.type.Symbol)
     */

    /**
     * @param one one
     * @param two two
     * @return 返回两个 {@link Symbol}之间的加权距离
     */
    public double getDistance(Symbol one, Symbol two)
    {
        return distances[one.byteValue()][two.byteValue()];
    }

    /*
     * (non-Javadoc)
     *
     * @see mobios.metric.WeightMatrix#getAlphabet()
     */

    /**
     * @return 返回定义在该替换矩阵上的 {@link Alphabet}
     */
    public Alphabet getAlphabet()
    {
        return alphabet;
    }
}
