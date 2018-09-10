package hzf.demo.imitate;

/**
 * Created by WTO on 2018/6/8 0008.
 *
 */
public abstract class HighContainer implements Iterable<Integer>, Cloneable
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

    public abstract HighContainer add(final int x);

    public abstract HighContainer remove(final int x);

    public abstract boolean contain(final int x);

    public abstract int cardinality();

    public abstract int getSizeInBytes();

    public abstract HighContainer and(final HighContainer x);

    public abstract HighContainer or(final HighContainer x);

    public abstract HighContainer andNot(final HighContainer x);

    protected static short highbits(int x) {
        return (short) (x >>> 16);
    }

    protected static short lowbits(int x) {
        return (short) (x & 0xFFFF);
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
