package hzf.demo.imitate;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Created by hzf on 2018/9/12 0012.
 *
 */
public class DirectBitmapContainer extends Container
{
    private ByteBuffer buffer = null;
    private int cardinality = 0;

    private static final int DEFAULT_BYTE = 8192;

    public DirectBitmapContainer()
    {
        this.buffer = ByteBuffer.allocateDirect(DEFAULT_BYTE);
        this.buffer.limit(8192);
    }

    protected long array(int idx)
    {
        int _idx = idx << 3;
        return buffer.getLong(_idx);
    }

    protected void array_update(int idx, long data)
    {
        int _idx = idx << 3;
        buffer.putLong(_idx, data);
    }

    @Override
    public Container add(short x)
    {
        int unsigned = toIntUnsigned(x);
        int idx = unsigned / 64;
        long p = array(idx);
        long nval = p | 1l << (unsigned % 64);
        array_update(idx, nval);
        cardinality += (p ^ nval) >>> x;
        return this;
    }

    @Override
    public Container remove(short x)
    {
        int unsigned = toIntUnsigned(x);
        int idx = unsigned / 64;
        long p = array(idx);
        long dataTmp = 1l << (unsigned % 64);
        long resultTmp = p & dataTmp;
        long result = p & (~resultTmp);

        if (result != p)    // 有变化，即本来位置上就没有对应的位
        {
            array_update(idx, result);
            cardinality --;
        }
        return this;
    }

    @Override
    public boolean contain(short x)
    {
        int unsigned = toIntUnsigned(x);
        return (array(unsigned / 64) & 1l << (unsigned % 64)) != 0 ;
    }

    @Override
    public int cardinality() {
        return cardinality;
    }

    @Override
    public int getSizeInBytes() {
        return 8196;
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
    public Container toNextContainer() {
        return null;
    }

    protected void loadData(DirectArrayContainer arrayContainer)
    {
        this.cardinality = arrayContainer.cardinality;
        for (int k = 0; k < arrayContainer.cardinality; ++k)
        {
            final short x = arrayContainer.array(k);
            int idx = toIntUnsigned(x) / 64;
            long p_val = array(idx);
            long n_val = p_val | (1L << x);
            array_update(idx, n_val);
        }
    }


    protected void loadData(DynScaleBitmapContainer container)
    {

    }

    @Override
    public Container clone() {
        return null;
    }

    @Override
    public Iterator<Short> iterator() {
        return null;
    }

    @Override
    public String toString() {
        return "DirectBitmapContainer{}";
    }
}
