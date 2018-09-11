package hzf.demo.imitate;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by huangzhenfeng on 2018/9/10.
 *
 */
public class DynHighContainer extends HighContainer
{
    private static final int keys_max = 2048;
    private long[] keys = null;      // 2048个
    private Container[] array = null;     // 65536个

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


    private int findIdx(int idx)
    {
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
    public HighContainer remove(int x)
    {
        if (keys == null) {
            return this;
        }

        short x_hb = highbits(x);
        short x_low = lowbits(x);
        int idx = toIntUnsigned(x_hb);
        int idx_k = idx >> 5;       // 除以 32
        long key = keys[idx_k];

        long high = key >> 32;

        if (high == 0) {
            return this;
        }

        int k_offset = idx - idx_k * 32;

        boolean exist = (high & 1l << k_offset) != 0;

        if (!exist) {
            return this;
        }

        long low = key & 4294967295l;       // 低位代表前面有多少个1，也就是代表着前面起码有low个数
        low += Long.bitCount(high & whereIdx[k_offset]);

        Container container = array[((int) low)];
        array[((int) low)] = container.remove(x_low);

        return this;
    }

    @Override
    public boolean contain(int x)
    {
        if (keys == null) {
            return false;
        }

        short x_hb = highbits(x);
        short x_low = lowbits(x);
        int idx = toIntUnsigned(x_hb);
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
    public int cardinality()
    {
        if (array != null)
        {
            int cardinality = 0;
            for (Container container : array)
            {
                cardinality += container.cardinality();
            }
            return cardinality;
        }
        return 0;
    }

    @Override
    public int getSizeInBytes()
    {
        if (array != null)
        {
            int sizeInBytes = 0;
            for (Container container : array)
            {
                sizeInBytes += container.getSizeInBytes();
            }
            return sizeInBytes + 2048 * 8;
        }

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
    public Iterator<Integer> iterator()
    {
        return new DynHighContainerIterator();
    }

    class DynHighContainerIterator implements Iterator<Integer>
    {
        private long[] _keys;
        private Container[] _array;
        private int min = 0;
        private BlockingQueue<Integer> datas = null;
        private int data = 0;

        private int array_idx = 0;
        private int key_idx = 0;
        private int key_loop_idx = -1;
        private long high;

        private DynHighContainerIterator()
        {
            if (keys == null)
            {
                this._keys = new long[keys_max];
                this._array = null;
            }
            else
            {
                this._keys = keys;
                this._array = array;
            }

            this.datas = new LinkedBlockingQueue<Integer>();
        }

        @Override
        public boolean hasNext()
        {
            Integer tmp = datas.poll();
            if (tmp != null)
            {
                data = tmp;
                return true;
            }
            else
            {
                key_loop_idx ++;
                while (key_idx < keys_max)
                {
                    this.high = _keys[key_idx] >> 32 & 4294967295l;
                    if (high != 0)
                    {
                        while (key_loop_idx < 32)
                        {
                            if ((high & 1l << key_loop_idx) != 0)
                            {
                                Container container = _array[array_idx];
                                array_idx ++;
                                min = (key_idx * 32 * 65536 + key_loop_idx * 65536);
                                putData(container);
                                data = datas.poll();
                                return true;
                            }

                            key_loop_idx++;
                        }
                    }
                    key_idx ++;
                    key_loop_idx = -1;
                }
            }
            return false;
        }

        @Override
        public Integer next()
        {
            return data;
        }

        private void putData(Container container)
        {
            Iterator<Short> iterator =  container.iterator();
            while (iterator.hasNext())
            {
                datas.add(min + (iterator.next() & 0xFFFF));
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static void main(String[] args)
    {
        DynHighContainer highContainer = new DynHighContainer();

        highContainer.add(5);
        highContainer.add(5);
        highContainer.add(2545);
        highContainer.add(5);
        highContainer.add(66);
        highContainer.add(176554458);

        System.out.println(highContainer.contain(66));
        System.out.println(highContainer.contain(555));
        System.out.println(highContainer.contain(176554458));


        Iterator<Integer> iterator = highContainer.iterator();

        while (iterator.hasNext())
        {
            System.out.println(iterator.next());
        }

        System.out.println("cardinality - " + highContainer.cardinality());
        System.out.println("sizeInBytes - " + highContainer.getSizeInBytes());



    }
}
