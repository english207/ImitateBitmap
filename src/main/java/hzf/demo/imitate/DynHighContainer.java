package hzf.demo.imitate;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by hzf on 2018/9/10.
 *
 */
public class DynHighContainer extends HighContainer
{
    private static final int keys_max = 2048;
    protected long[] keys = null;      // 2048个
    protected Container[] array = null;     // 65536个
    protected short limit = 0;

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
            array = new Container[8];
            this.array[low] = new ArrayContainer();
            limit = 0;
            return;
        }

        if (low > limit)    //low一定是比上一个最大的low要大1
        {
            if (low == array.length)                // 需要扩容
            {
                this.array = Arrays.copyOf(array, low + 8);
            }
            this.array[low] = new ArrayContainer();
            limit = (short) low;
        }
        else if (low <= limit)                       // 从中间往后移动
        {
            if (limit == array.length - 1)          // 扩容
            {
                this.array = Arrays.copyOf(array, array.length + 8);
                System.arraycopy(array, low, array, low + 1, limit - low + 1);
                this.array[low] = new ArrayContainer();
                limit ++;
            }
            else
            {
                System.arraycopy(array, low, array, low + 1, limit - low + 1);
                this.array[low] = new ArrayContainer();
                limit ++;
            }
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
        container = container.remove(x_low);
        array[((int) low)] = container;

        int cardinality_new = container.cardinality();
        if (cardinality_new == 0)           // 代表着这个容器里面的数据已经空了
        {
            Container[] newArray = new Container[array.length - 1];
            if (low == 0)                           // head
            {
                System.arraycopy(this.array, 1, newArray, 0, (array.length - 1));
            }
            else if (low == array.length - 1)       // tail
            {
                System.arraycopy(this.array, 0, newArray, 0, (array.length - 1));
            }
            else
            {
                System.arraycopy(this.array, 0, newArray, 0, (int) low);
                System.arraycopy(this.array, (int) (low + 1), newArray, (int) low, (int) (array.length - low - 1));
            }
            this.array = newArray;

            long keyTmp = key & (1l << (k_offset + 32));
            long key_result = key & (~keyTmp);
            keys[idx_k] = key_result;

            for (int i = idx_k + 1 ; i < keys_max ; i ++)
            {
                keys[i] -= 1;
            }
        }

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
                if (container == null)
                {
                    break;
                }
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
            for (int i = 0; i < limit; i++) {
                sizeInBytes += array[i].getSizeInBytes();
            }
            return sizeInBytes + 2048 * 8;
        }

        return 0;
    }

    @Override
    public HighContainer and(HighContainer x)
    {
        if (x instanceof DynHighContainer)
        {
            return and((DynHighContainer)x);
        }
        return null;
    }


    private long getKey(long[] _keys, int idx) {
        return _keys != null ? _keys[idx] : 0;
    }

    private HighContainer checkIsEmpty(DynHighContainer dhc, int op)
    {
        DynHighContainer newContainer = null;
        if (this.keys == null)
        {
            if (op == 1)       // or
            {
                if (dhc.keys != null)
                {
                    newContainer = new DynHighContainer();
                    newContainer.keys = keysClone(dhc.keys);
                    newContainer.array = containerClone(dhc.array);
                }
            }
            else if ( op == 0 || op == 2 )       // andNot
            {
                newContainer = new DynHighContainer();
            }
        }
        else if (dhc.keys == null)
        {
            if (op == 0)       // and
            {
                newContainer = new DynHighContainer();
            }
            else if (op == 1)       // or
            {
                newContainer = new DynHighContainer();
                newContainer.keys = keysClone(this.keys);
                newContainer.array = containerClone(this.array);
            }
            else if (op == 2)       // andNot
            {
                newContainer = new DynHighContainer();
                newContainer.keys = keysClone(this.keys);
                newContainer.array = containerClone(this.array);
            }
        }
        return newContainer;
    }

    private HighContainer and(DynHighContainer dhc)
    {
        DynHighContainer newDsbc = (DynHighContainer) checkIsEmpty(dhc, 0);
        if ( newDsbc != null) {
            return newDsbc;
        }

        long[] newKeys = new long[2048];
        Container[] newArray = new Container[65536];

        int idx = 0;
        int low_size = 0;
        for (int i = 0; i < keys.length; i++)
        {
            long key1 = getKey(keys, i);
            long key2 = getKey(dhc.keys, i);

            long high1 = key1 >> 32 & 4294967295l;
            long high2 = key2 >> 32 & 4294967295l;
            long high_idx = high1 & high2;
            if (high_idx == 0)
            {
                int l = i + 1;
                if (l < 32) {
                    newKeys[l] = newKeys[l] + low_size;
                }
                continue;
            }

            long high1_low = key1 & 4294967295l;
            long high2_low = key2 & 4294967295l;
            int high_each = 0;
            int size = 0;
            int bitCount = Long.bitCount(high_idx);
            int findBit = 0;

            for (int j = 0; j < 32 && findBit < bitCount; j++)
            {
                long high_idx_tmp = 1l << j;
                if (high_idx_tmp > high_idx) {
                    break;
                }

                if ( (high_idx & high_idx_tmp) != 0 )     // 对应位都存在
                {
                    findBit++;
                    for (; high_each < j; high_each++)
                    {
                        high1_low += (high1 & 1 << high_each) != 0 ? 1 : 0;
                        high2_low += (high2 & 1 << high_each) != 0 ? 1 : 0;
                    }

                    // 如果 对应位上不为0，则可以进行取值，如果为0，则代表着不需要进行取值，直接用0计算
                    Container data1 = (high1 & high_idx_tmp) != 0 ? (high1_low >= this.array.length ? new DynScaleBitmapContainer() : this.array[((int) high1_low)]) : new DynScaleBitmapContainer();
                    Container data2 = (high2 & high_idx_tmp) != 0 ? (high2_low >= dhc.array.length ? new DynScaleBitmapContainer() : dhc.array[((int) high2_low)]) : new DynScaleBitmapContainer();
                    Container result = data1.and(data2);

                    if (result.cardinality() == 0) {  // 对应位存在，但计算结果不存在，将key设置为0
                        continue;
                    }

                    size++;
                    newArray[idx++] = result;
                    newKeys[i] |= 1l << (j + 32);
                }
            }

            low_size += size;
            int l = i + 1;
            if (l < keys_max) {
                newKeys[l] = newKeys[l] + low_size;
            }
        }

        newDsbc = new DynHighContainer();
        if (idx > 0)
        {
            Container[] newArrayTmp = new Container[idx];
            System.arraycopy(newArray, 0, newArrayTmp, 0, idx);
            newDsbc.keys = newKeys;
            newDsbc.array = newArrayTmp;
        }
        return newDsbc;
    }

    @Override
    public HighContainer or(HighContainer x)
    {
        if (x instanceof DynHighContainer)
        {
            return or((DynHighContainer) x);
        }
        return null;
    }

    private HighContainer or(DynHighContainer dhc)
    {
        DynHighContainer newDhc = (DynHighContainer) checkIsEmpty(dhc, 0);
        if ( newDhc != null) {
            return newDhc;
        }

        long[] newKeys = new long[keys_max];
        Container[] newArray = new Container[65536];

        int idx = 0;            // 记录低位有多少array
        int low_size = 0;       // 记录低位有多少个
        for (int i = 0; i < keys.length; i++)
        {
            long key1 = this.keys[i];
            long key2 = dhc.keys[i];

            long high1 = key1 >> 32 & 4294967295l;
            long high2 = key2 >> 32 & 4294967295l;
            long high_idx = high1 | high2;
            if (high_idx == 0) {
                int l = i + 1;
                if (l < 32) {
                    newKeys[l] = newKeys[l] + low_size;
                }
                continue;
            }

            long high1_low = key1 & 4294967295l;
            long high2_low = key2 & 4294967295l;
            int high_each = 0;
            int bitCount = Long.bitCount(high_idx);
            int findBit = 0;

            for (int j = 0; j < 32 && findBit < bitCount; j++)
            {
                long high_idx_tmp = 1l << j;
                if (high_idx_tmp > high_idx) {
                    break;
                }

                if ( (high_idx & high_idx_tmp) != 0 )       // 对应位中有一个存在
                {
                    findBit++;
                    for (; high_each < j; high_each++)
                    {
                        high1_low += (high1 & 1 << high_each) != 0 ? 1 : 0;
                        high2_low += (high2 & 1 << high_each) != 0 ? 1 : 0;
                    }

                    // 如果 对应位上不为0，则可以进行取值，如果为0，则代表着不需要进行取值，直接用0计算
                    Container data1 = (high1 & high_idx_tmp) != 0 ? (high1_low >= this.array.length ? new DynScaleBitmapContainer() : this.array[((int) high1_low)]) : new DynScaleBitmapContainer();
                    Container data2 = (high2 & high_idx_tmp) != 0 ? (high2_low >= dhc.array.length ? new DynScaleBitmapContainer() : dhc.array[((int) high2_low)]) : new DynScaleBitmapContainer();
                    Container result = data1.or(data2);
                    if (result.cardinality() == 0) {  // 对应位存在，但计算结果不存在，将key设置为0
                        continue;
                    }

                    newArray[idx++] = result;
                    newKeys[i] |= 1l << (j + 32);
                }
            }

            low_size += Long.bitCount(newKeys[i] >>> 32);
            int l = i + 1;
            if (l < keys_max) {
                newKeys[l] = newKeys[l] + low_size;
            }
        }

        newDhc = new DynHighContainer();
        if (idx > 0)
        {
            Container[] newArrayTmp = new Container[idx];
            System.arraycopy(newArray, 0, newArrayTmp, 0, idx);
            newDhc.keys = newKeys;
            newDhc.array = newArrayTmp;
        }
        return newDhc;
    }

    @Override
    public HighContainer andNot(HighContainer x)
    {
        if (x instanceof DynHighContainer)
        {
            return andNot((DynHighContainer) x);
        }
        return null;
    }

    private HighContainer andNot(DynHighContainer dhc)
    {
        DynHighContainer newDhc = (DynHighContainer) checkIsEmpty(dhc, 0);
        if ( newDhc != null) {
            return newDhc;
        }

        long[] newKeys = new long[keys_max];
        Container[] newArray = new Container[65536];

        int idx = 0;
        int low_size = 0;       // 记录低位有多少个
        for (int i = 0; i < keys.length; i++)
        {
            long key1 = this.keys[i];
            long key2 = dhc.keys[i];

            long high1 = key1 >> 32 & 4294967295l;
            long high2 = key2 >> 32 & 4294967295l;
            long high_idx = high1 & high2;

            if (high_idx == 0)
            {
                if (high1 != 0)
                {
                    int high_low = (int)(key1 & 4294967295l);
                    int idxSize = Long.bitCount(high1);
                    for (int j = 0; j < idxSize; j++) {
                        newArray[idx++] = this.array[j + high_low];
                    }
                    newKeys[i] += high1 << 32;
                    low_size += idxSize;
                }

                int l = i + 1;
                if (l < 32) {
                    newKeys[l] = newKeys[l] + low_size;
                }
                continue;
            }

            long high1_low = key1 & 4294967295l;
            long high2_low = key2 & 4294967295l;
            int high_each = 0;

            for (int j = 0; j < 32; j++)
            {
                long high_idx_tmp = 1l << j;
                if (high_idx_tmp > high1) {
                    break;
                }

                if ( (high_idx & high_idx_tmp) != 0 )       // 对应位中有一个存在
                {
                    for (; high_each < j; high_each++)
                    {
                        high1_low += (high1 & 1 << high_each) != 0 ? 1 : 0;
                        high2_low += (high2 & 1 << high_each) != 0 ? 1 : 0;
                    }

                    // 如果 对应位上不为0，则可以进行取值，如果为0，则代表着不需要进行取值，直接用0计算
                    Container data1 = (high1 & high_idx_tmp) != 0 ? (high1_low >= this.array.length ? new DynScaleBitmapContainer() : this.array[((int) high1_low)]) : new DynScaleBitmapContainer();
                    Container data2 = (high2 & high_idx_tmp) != 0 ? (high2_low >=  dhc.array.length ? new DynScaleBitmapContainer() :  dhc.array[((int) high2_low)]) : new DynScaleBitmapContainer();
                    Container result = data1.andNot(data2);

                    if (result.cardinality() == 0) {  // 清除之后当前位上已经没有数了
                        continue;
                    }

                    result = (result.cardinality() == data1.cardinality() ? data1 : result);
                    newArray[idx++] = result; // 判断是否有变化
                    newKeys[i] |= 1l << (j + 32);
                    low_size += Long.bitCount(newKeys[i] >> 32);
                }
                else                                       // 对应位中不存在
                {
                    if ( (high1 & high_idx_tmp) != 0 )
                    {
                        Container data = high1_low >= this.array.length ? new DynScaleBitmapContainer() : this.array[(idx)];
                        newArray[idx++] = data;
                        newKeys[i] |= 1l << (j + 32);
                        low_size += Long.bitCount(newKeys[i] >> 32);
                    }
                }
            }

            int l = i + 1;
            if (l < keys_max) {
                newKeys[l] = newKeys[l] + low_size;
            }
        }

        newDhc = new DynHighContainer();
        if (low_size > 0)
        {
            Container[] newArrayTmp = new Container[low_size];
            System.arraycopy(newArray, 0, newArrayTmp, 0, low_size);
            newDhc.keys = newKeys;
            newDhc.array = newArrayTmp;
        }
        return newDhc;
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

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        int total = 0;
        Iterator<Integer> iterator = iterator();
        while (iterator.hasNext())
        {
            total ++;
            if (total == 1000)
            {
                sb.append("...");
                sb.append(",");
                break;
            }
            sb.append(iterator.next());
            sb.append(",");
        }

        if (sb.length() > 1)
        {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args)
    {
        DynHighContainer highContainer = new DynHighContainer();
        DynHighContainer highContainer2 = new DynHighContainer();

        highContainer.add(5);
        highContainer.add(5);
        highContainer.add(2545);
        highContainer.add(5);
        highContainer.add(66);
        highContainer.add(176554458);

        highContainer2.add(8);
        highContainer2.add(5);
        highContainer2.add(176554458);
        HighContainer highContainer3 = highContainer.and(highContainer2);

        System.out.println(highContainer);
        System.out.println(highContainer3);

//
//        System.out.println("cardinality - " + highContainer.cardinality());
//        System.out.println("sizeInBytes - " + highContainer.getSizeInBytes());
//
//
//        System.out.println("cardinality - " + highContainer3.cardinality());
//        System.out.println("sizeInBytes - " + highContainer3.getSizeInBytes());



    }
}
