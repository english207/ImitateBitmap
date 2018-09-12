package hzf.demo.imitate;

import org.roaringbitmap.Util;

import java.util.Iterator;

/**
 * Created by WTO on 2018/9/12 0012.
 *
 */
public class BitmapContainer extends Container
{
    private long[] array = null;
    private int cardinality = 0;

    public BitmapContainer()
    {
        this.array = new long[1024];
    }

    @Override
    public Container add(short x)
    {
        int unsigned = toIntUnsigned(x);
        int idx = unsigned / 64;
        long p = array[idx];
        long nval = p | 1l << (unsigned % 64);
        array[idx] = nval;
        cardinality += (p ^ nval) >>> x;
        return this;
    }

    @Override
    public Container remove(short x) {
        return null;
    }

    @Override
    public boolean contain(short x) {
        return false;
    }

    @Override
    public int cardinality() {
        return 0;
    }

    @Override
    public int getSizeInBytes() {
        return 0;
    }

    @Override
    public Container and(Container x) {
        return null;
    }

    @Override
    public Container or(Container x) {
        return null;
    }

    @Override
    public Container andNot(Container x) {
        return null;
    }

    @Override
    public Container clone() {
        return null;
    }

    @Override
    public Iterator<Short> iterator() {
        return null;
    }
}
