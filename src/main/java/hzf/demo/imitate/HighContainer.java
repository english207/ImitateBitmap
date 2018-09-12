package hzf.demo.imitate;

import java.util.Arrays;

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

    public long time;

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

    protected static long[] keysClone(long[] keys)
    {
        if (keys == null)
        {
            return null;
        }
        return Arrays.copyOf(keys, keys.length);
    }

    protected static Container[] containerClone(Container[] containers)
    {
        if (containers == null)
        {
            return null;
        }
        Container[] newContainers = new Container[containers.length];
        for (int i = 0; i < containers.length; i++)
        {
            Container container = containers[i];
            Container newContainer = new DynScaleBitmapContainer();
            newContainer = newContainer.or(container);
            newContainers[i] = newContainer;
        }
        return newContainers;
    }

    public static void main(String[] args) {

        long l = 0;
        for (int i = 0; i < 64; i++) {
            l |= 1l << i;
            System.out.println(Long.toBinaryString(l) + " - " + l);
        }



    }
}
