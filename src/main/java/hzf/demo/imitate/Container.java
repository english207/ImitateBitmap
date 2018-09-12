package hzf.demo.imitate;

/**
 * Created by WTO on 2018/6/8 0008.
 *
 */
public abstract class Container implements Iterable<Short>, Cloneable
{
    public static final int len = 6;
    public static final int[] whereIdx = new int[32];
    static
    {
        int l = 0;
        for (int i = 0; i < 32; i++)
        {
            l |= l + ((1 << i - 1) & 2147483647);
            whereIdx[i] = l;
        }
    }

    public static long time;

    public abstract Container add(final short x);

    public abstract Container remove(final short x);

    public abstract boolean contain(final short x);

    public abstract int cardinality();

    public abstract int getSizeInBytes();

    public abstract Container and(final Container x);

    public abstract Container or(final Container x);

    public abstract Container andNot(final Container x);

    @Override
    public abstract Container clone();

    protected static short highbits(int x) {
        return (short) (x >>> 16);
    }

    protected static int toIntUnsigned(short x) {
        return x & 0xFFFF;
    }

    public static void main(String[] args) {

        long l = 0;
        for (int i = 0; i < 64; i++) {
            l |= 1l << i;
            System.out.println(Long.toBinaryString(l) + " - " + l);
        }



    }
}
