package hzf.demo.imitate;

/**
 * Created by hzf on 2018/6/8 0008.
 *
 */
public abstract class Container implements Iterable<Short>, Cloneable
{
    public static final int len = 6;
    public static int[] whereIdx = new int[32];
    static
    {
        int l = 0;
        for (int i = 0; i < 32; i++)
        {
            l |= l + ((1 << i - 1) & 2147483647);
            whereIdx[i] = l;
        }
    }

    public abstract void add(final short x);

    public abstract void remove(short x);

    public abstract boolean contain(final short x);

    public abstract int getSizeInBytes();

    public abstract int cardinality();

    public abstract Container and(final Container x);

    public abstract Container or(final Container x);

    public abstract Container andNot(final Container x);

    protected static short highbits(int x) {
        return (short) (x >>> 16);
    }

    protected static int toIntUnsigned(short x) {
        return x & 0xFFFF;
    }
}
