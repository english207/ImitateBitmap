package hzf.demo.imitate;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by huangzhenfeng on 2018/9/10.
 *
 */
public class DynHighContainer extends HighContainer
{
    private static final int keys_max = 2048;
    private long[] keys = null;      // 2048个
    private Container[] array = null;     // 65536个
    private int cardinality = 0;


    @Override
    public HighContainer add(int x)
    {
        if (keys == null) {
            keys = new long[keys_max];
        }
        
        short hb = highbits(x);
        short low = lowbits(x);
        int unsigned = toIntUnsigned(hb);
        int idx = findIdx(unsigned);

        Container container = array[idx];
        array[idx] = container.add(low);

        return this;
    }


    private int findIdx(int unsigned)
    {
        int idx = unsigned >> len;
        int idx_k = idx >> 5;
        long key = keys[idx_k];

        long high = key >> 32;
        int k_offset = idx - idx_k * 32;

        long low = key & 4294967295l;       // 低位代表前面有多少个1，也就是代表着前面起码有low个数
        if (high != 0)
        {
            low += Long.bitCount(high & whereIdx[k_offset]);
        }

        boolean exist = (high & 1l << k_offset) != 0;   // 判断key上有没有对应的位，如果有，则直接计算，没有则要扩容以及更新high 和 low
        if (!exist)
        {
            increaseCapacity((int) low);
            update(idx_k, k_offset + 32);
        }
        return (int) low;
    }

    private void increaseCapacity(int low)
    {
        if (array == null) {
            array = new Container[1];
            this.array[low] = new DynScaleBitmapContainer();
            return;
        }
        if (low >= array.length)
        {
            this.array = Arrays.copyOf(array, low + 1);
            this.array[low] = new DynScaleBitmapContainer();
        }
        else if (low < array.length)
        {
            this.array = Arrays.copyOf(array, array.length + 1);
            System.arraycopy(array, low, array, low + 1, array.length - low - 1);
            this.array[low] = new DynScaleBitmapContainer();
        }
    }

    private void update(int idx_k, int update_offset) {
        update(keys, idx_k, update_offset, 1);
    }

    private void update(long[] _keys, int idx_k, int update_offset, int size)
    {
        _keys[idx_k] |= 1l << update_offset;
        for (int i = idx_k + 1; i < keys_max; i++) {
            _keys[i] = _keys[i] + size;
        }
    }

    @Override
    public HighContainer remove(int x) {
        return null;
    }

    @Override
    public boolean contain(int x)
    {
        if (keys == null) {
            return false;
        }

        short x_hb = highbits(x);
        short x_low = lowbits(x);
        int unsigned = toIntUnsigned(x_hb);
        int idx = unsigned >> len;  // 除以 64
        int idx_k = idx >> 5;       // 除以 32
        long key = keys[idx_k];

        long high = key >> 32;

        if (high == 0) {
            return false;
        }

        int k_offset = idx - idx_k * 32;

        boolean exist = (high & 1l << k_offset) != 0;

        if (!exist) {
            return false;
        }

        long low = key & 4294967295l;       // 低位代表前面有多少个1，也就是代表着前面起码有low个数
        low += Long.bitCount(high & whereIdx[k_offset]);

        Container container = array[((int) low)];
        return container.contain(x_low);
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
    public HighContainer and(HighContainer x) {
        return null;
    }

    @Override
    public HighContainer or(HighContainer x) {
        return null;
    }

    @Override
    public HighContainer andNot(HighContainer x) {
        return null;
    }

    @Override
    public Iterator<Integer> iterator() {
        return null;
    }

    public static void main(String[] args)
    {
        DynHighContainer highContainer = new DynHighContainer();

        highContainer.add(5);
        highContainer.add(66);

        System.out.println(highContainer.contain(66));
        System.out.println(highContainer.contain(555));





    }
}
