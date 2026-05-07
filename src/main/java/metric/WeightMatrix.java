package metric;

import db.type.Alphabet;
import db.type.Fragment;
import db.type.Sequence;
import db.type.Symbol;

import java.io.Serializable;

///**
// * A <code>WeightMatrix</code> is the substitution matrix of an {@link Alphabet}of {@link Symbol}
// * objects. It defines the distance between each pair of <code>Symbol</code>s of the
// * Alphabet..  Using a WeightMatrix, a weighted edit distance metric can be defined
// * on{@link Sequence} or {@link Fragment} objects.
// *
// * @author Jack, Rui Mao, Weijia Xu, Willard
// * @version 2004.03.02
// */

/**
 * {@code WeightMatrix} 是{@link Symbol}的{@link Alphabet}的替换矩阵。
 * <p>
 * {@code WeightMatrix}定义了{@link Alphabet}的{@link Symbol}对之间的距离。使用{@code WeightMatrix}可以在{@link Sequence}或者
 * {@link Fragment}上定义加权编辑距离。
 */
public interface WeightMatrix extends Serializable
{
    //    /**
    //     * @return the {@link Alphabet} over which this matrix is defined.
    //     */

    /**
     * @return 定义在 {@code matrix}上的 {@link Alphabet}
     */
    Alphabet getAlphabet();

    //    /**
    //     * @param one one
    //     * @param two two
    //     * @return the distance between two {@link Symbol} objects in the {@link Alphabet} over which
    //     *         this matrix is defined.
    //     */

    /**
     * 返回两个符号间的距离
     *
     * @param one one
     * @param two two
     * @return 返回距离值
     */
    double getDistance(Symbol one, Symbol two);

}
