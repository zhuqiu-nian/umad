package db.type;

import java.io.Serializable;

/**
 * 一个{@link Symbol}集合，可以在该{@link Symbol}集合的基础上构建相应的数据类型。
 */
public class Alphabet implements Serializable
{

    private static final long serialVersionUID = -3200343173661145001L;

    Symbol[] alphabet;
    int      distinctSize;

    /**
     * 构造函数
     *
     * @param alphabet     {@link Symbol}集合 {@code alphabet}
     * @param distinctSize {@code Alphabet} 的大小
     */
    public Alphabet(Symbol[] alphabet, int distinctSize)
    {
        this.alphabet     = alphabet;
        this.distinctSize = distinctSize;
    }

    /**
     * 获取创建{@code Alphabet}的{@link Symbol}集合的实际大小
     *
     * @return 返回创建{@code Alphabet}的{@link Symbol}集合的实际大小
     */
    public int size()
    {
        return alphabet.length;
    }

    /**
     * 获取该索引值上的 {@link Symbol}
     *
     * @param index 索引值
     * @return 返回该索引值上的 {@link Symbol}
     */
    public Symbol get(int index)
    {
        return alphabet[index];
    }

    /**
     * 获取{@code Alphabet}的大小
     *
     * @return 返回{@code Alphabet}的大小
     */
    public int distinctSize()
    {
        return distinctSize;
    }
}
