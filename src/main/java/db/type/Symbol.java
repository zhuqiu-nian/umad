package db.type;

import java.io.Externalizable;


/**
 * 生物序列的中的字符需要实现 {@code  Symbol} 接口。
 * <p>
 * Since biosequences often have a long life in an external representation, {@code Symbol }
 * alphabets should have a similarly long life, suggesting that they be immutable and easily
 * serializable. In this package, this is accomplished by implementing {@code Symbol } sets as
 * java enums. In addition, {@link Sequence}s using {@code Symbol } alphabets include a
 * {@code static public final } {@link Alphabet} instance, named {@code ALPHABET },
 * that references the enumeration. For example, class {@link Peptide} contains the the class
 * {@link Peptide.AminoAcid} that implements {@code Symbol }for the common amino acids.
 */
public interface Symbol extends Externalizable
{


    Symbol getSymbol(String s);


    byte byteValue();


    String stringValue();
}
