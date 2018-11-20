package hzf.demo.imitate;

import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.Util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by hzf on 2018/6/11.
 *
 *      动态伸缩bitmap
 */
public class DynScaleBitmapContainer extends Container implements Cloneable
{
    private long[] keys = null;
    private long[] array = null;     // 1024个
    private int cardinality = 0;
    private short limit = 0;

    public DynScaleBitmapContainer() {
    }

    public DynScaleBitmapContainer(long[] newKeys, long [] newArray, int newCard)
    {
        if (newKeys != null)
        {
            this.keys = Arrays.copyOf(newKeys, newKeys.length);
            this.array = Arrays.copyOf(newArray, newArray.length);
            this.cardinality = newCard;
        }
    }

    @Override
    public Container add(short x)
    {
        if (keys == null) {
            keys = new long[32];
        }
        int unsigned = toIntUnsigned(x);
        int idx = findIdx(unsigned);

        if (idx >= array.length)
        {
            idx = 0;
        }
        long p = array[idx];
        long nval = p | 1l << (unsigned % 64);
        array[idx] = nval;
        cardinality += (p ^ nval) >>> x;

        return this;
    }

    @Override
    public Container remove(short x)
    {
        if (keys == null) {
            return this;
        }
        int unsigned = toIntUnsigned(x);
        int idx = unsigned >> len;  // 除以 64
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

        long p = array[(int) low];
        long dataTmp = 1l << (unsigned % 64);
        long resultTmp = p & dataTmp;
        long result = p & (~resultTmp);

        if (result == p) {   // 没变化，即本来位置上就没有对应的位
            return this;
        }
        array[((int) low)] = result;    // 将对应位修改成对应数字
        if (result == 0)                            // 清除之后当前位上已经没有数了
        {
            long[] newArray = new long[array.length - 1];
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

            // 接下来处理清除成功之后的变化   1，修改key，2，移动array的位置
            long keyTmp = key & (1l << (k_offset + 32));
            long key_result = key & (~keyTmp);
            keys[idx_k] = key_result;

            for (int i = idx_k + 1 ; i < 32 ; i ++)
            {
                keys[i] -= 1;
            }
        }
        cardinality --;

        if (cardinality == 0) {
            this.keys = null;
            this.array = null;
        }

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
            array = new long[4];
            limit = 0;
            return;
        }

        if (low > limit)    //low一定是比上一个最大的low要大1
        {
            if (low == array.length)                // 需要扩容
            {
                this.array = Arrays.copyOf(array, low + 4);
            }
            limit = (short) low;
        }
        else if (low <= limit)                       // 从中间往后移动
        {
            if (limit == array.length - 1)          // 扩容
            {
                this.array = Arrays.copyOf(array, array.length + 4);
                System.arraycopy(array, low, array, low + 1, limit - low + 1);
                this.array[low] = 0;
                limit ++;
            }
            else
            {
                System.arraycopy(array, low, array, low + 1, limit - low + 1);
                this.array[low] = 0;
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
        for (int i = idx_k + 1; i < 32; i++) {
            _keys[i] = _keys[i] + size;
        }
    }

    @Override
    public boolean contain(short x)
    {
        if (keys == null) {
            return false;
        }
        int unsigned = toIntUnsigned(x);
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

        return (array[((int) low)] & 1l << (unsigned % 64)) != 0 ;
    }

    @Override
    public int cardinality() {
        return cardinality;
    }

    public int getSizeInBytes()
    {
        int size = (keys != null ? keys.length * 8 : 0) + (array != null ? array.length * 8 : 0);
        return size + 4;
    }

    private Container checkIsEmpty(DynScaleBitmapContainer dsbc, int op)
    {
        DynScaleBitmapContainer newContainer = null;
        if (this.keys == null)
        {
            if (op == 1)       // or
            {
                if (dsbc.keys != null)
                {
                    newContainer = new DynScaleBitmapContainer();
                    newContainer.keys = dsbc.keys.clone();
                    newContainer.array = dsbc.array.clone();
                    newContainer.cardinality = dsbc.cardinality;
                }
            }
            else if (op == 2)       // andNot
            {
                newContainer = new DynScaleBitmapContainer();
            }
        }
        else if (dsbc.keys == null)
        {
            if (op == 1)       // or
            {
                newContainer = new DynScaleBitmapContainer();
                newContainer.keys = this.keys.clone();
                newContainer.array = this.array.clone();
                newContainer.cardinality = this.cardinality;
            }
            else if (op == 2)       // andNot
            {
                newContainer = new DynScaleBitmapContainer();
                newContainer.keys = this.keys.clone();
                newContainer.array = this.array.clone();
                newContainer.cardinality = this.cardinality;
            }
        }
        return newContainer;
    }

    @Override
    public Container and(Container x)
    {
        DynScaleBitmapContainer dsbc = (DynScaleBitmapContainer) x;
        DynScaleBitmapContainer newDsbc = (DynScaleBitmapContainer) checkIsEmpty(dsbc, 0);
        if ( newDsbc != null) {
            return newDsbc;
        }

        long[] newKeys = new long[32];
        long[] newArray = new long[1024];
        short newCardinality = 0;

        int idx = 0;
        int low_size = 0;
        for (int i = 0; i < keys.length; i++)
        {
            long key1 = getKey(keys, i);
            long key2 = getKey(dsbc.keys, i);

            long high1 = key1 >> 32 & 4294967295l;
            long high2 = key2 >> 32 & 4294967295l;
            long high_idx = high1 & high2;
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
                    long data1 = (high1 & high_idx_tmp) != 0 ? (high1_low >= this.array.length ? 0l : this.array[((int) high1_low)]) : 0;
                    long data2 = (high2 & high_idx_tmp) != 0 ? (high2_low >= dsbc.array.length ? 0l : dsbc.array[((int) high2_low)]) : 0;
                    long result = data1 & data2;

                    if (result == 0) {  // 对应位存在，但计算结果不存在，将key设置为0
                        continue;
                    }

                    size++;
                    newArray[idx++] = result;
                    newKeys[i] |= 1l << (j + 32);
                    newCardinality += Long.bitCount(result);
                }
            }

            low_size += size;
            int l = i + 1;
            if (l < 32) {
                newKeys[l] = newKeys[l] + low_size;
            }
        }

        newDsbc = new DynScaleBitmapContainer();
        if (idx > 0)
        {
            int len = idx / 8 * 8;
            if (len < idx)
            {
                len += 8;
            }

            long[] newArrayTmp = new long[len];
            System.arraycopy(newArray, 0, newArrayTmp, 0, idx);
            newDsbc.keys = newKeys;
            newDsbc.array = newArrayTmp;
            newDsbc.cardinality = newCardinality;
            newDsbc.limit = (short) idx;
        }
        return newDsbc;
    }

    @Override
    public Container or(Container x)
    {
        DynScaleBitmapContainer dsbc = (DynScaleBitmapContainer) x;
        DynScaleBitmapContainer newDsbc = (DynScaleBitmapContainer) checkIsEmpty(dsbc, 1);
        if ( newDsbc != null) {
            return newDsbc;
        }

        long[] newKeys = new long[32];
        long[] newArray = new long[1024];
        short newCardinality = 0;

        int idx = 0;            // 记录低位有多少array
        int low_size = 0;       // 记录低位有多少个
        for (int i = 0; i < keys.length; i++)
        {
            long key1 = this.keys[i];
            long key2 = dsbc.keys[i];

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
                    long data1 = (high1 & high_idx_tmp) != 0 ? (high1_low >= this.array.length ? 0l : this.array[((int) high1_low)]) : 0;
                    long data2 = (high2 & high_idx_tmp) != 0 ? (high2_low >= dsbc.array.length ? 0l : dsbc.array[((int) high2_low)]) : 0;
                    long result = data1 | data2;
                    if (result == 0) {  // 对应位存在，但计算结果不存在，将key设置为0
                        continue;
                    }

                    newArray[idx++] = result;
                    newKeys[i] |= 1l << (j + 32);

                    newCardinality += Long.bitCount(result);
                }
            }

            low_size += Long.bitCount(newKeys[i] >>> 32);
            int l = i + 1;
            if (l < 32) {
                newKeys[l] = newKeys[l] + low_size;
            }
        }

        newDsbc = new DynScaleBitmapContainer();
        if (idx > 0)
        {
            int len = idx / 8 * 8;
            if (len < idx)
            {
                len += 8;
            }

            long[] newArrayTmp = new long[len];
            System.arraycopy(newArray, 0, newArrayTmp, 0, idx);
            newDsbc.keys = newKeys;
            newDsbc.array = newArrayTmp;
            newDsbc.cardinality = newCardinality;
            newDsbc.limit = (short) idx;
        }
        return newDsbc;
    }

    @Override
    public Container andNot(Container x)
    {
        DynScaleBitmapContainer dsbc = (DynScaleBitmapContainer) x;
        DynScaleBitmapContainer newDsbc = (DynScaleBitmapContainer) checkIsEmpty(dsbc, 2);
        if ( newDsbc != null) {
            return newDsbc;
        }

        long[] newKeys = new long[32];
        long[] newArray = new long[1024];
        short newCardinality;

        int idx = 0;
        int low_size = 0;       // 记录低位有多少个
        for (int i = 0; i < keys.length; i++)
        {
            long key1 = this.keys[i];
            long key2 = dsbc.keys[i];

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
                    long data1 = (high1 & high_idx_tmp) != 0 ? (high1_low >= this.array.length ? 0l : this.array[((int) high1_low)]) : 0;
                    long data2 = (high2 & high_idx_tmp) != 0 ? (high2_low >= dsbc.array.length ? 0l : dsbc.array[((int) high2_low)]) : 0;
                    long resultTmp = data1 & data2;
                    long result = data1 & (~resultTmp);

                    if (result == 0) {  // 清除之后当前位上已经没有数了
                        continue;
                    }

                    result = (result == data1 ? data1 : result);
                    newArray[idx++] = result; // 判断是否有变化
                    newKeys[i] |= 1l << (j + 32);
                    low_size += Long.bitCount(newKeys[i] >> 32);
                }
                else                                       // 对应位中不存在
                {
                    if ( (high1 & high_idx_tmp) != 0 )
                    {
                        long data = high1_low >= this.array.length ? 0l : this.array[(idx)];
                        newArray[idx++] = data;
                        newKeys[i] |= 1l << (j + 32);
                        low_size += Long.bitCount(newKeys[i] >> 32);
                    }
                }
            }

            int l = i + 1;
            if (l < 32) {
                newKeys[l] = newKeys[l] + low_size;
            }
        }

        newCardinality = (short) low_size;
        newDsbc = new DynScaleBitmapContainer();
        if (low_size > 0)
        {
            int len = low_size / 8 * 8;
            if (len < low_size)
            {
                len += 8;
            }

            long[] newArrayTmp = new long[low_size];
            System.arraycopy(newArray, 0, newArrayTmp, 0, low_size);
            newDsbc.keys = newKeys;
            newDsbc.array = newArrayTmp;
            newDsbc.cardinality = newCardinality;
            newDsbc.limit = (short) low_size;
        }
        return newDsbc;
    }

    @Override
    public Container toNextContainer() {
        return null;
    }


    private long getKey(long[] _keys, int idx) {
        return _keys != null ? _keys[idx] : 0;
    }

    @Override
    public Iterator<Short> iterator() {
        return new DynScaleBitmapContainerIterator();
    }

    class DynScaleBitmapContainerIterator implements Iterator<Short>
    {
        private long[] _keys;
        private long[] _array;
        private short min = 0;
        private BlockingQueue<Short> datas = null;
        private short data = 0;

        private int array_idx = 0;
        private int key_idx = 0;
        private int key_loop_idx = -1;
        private long high;

        private DynScaleBitmapContainerIterator()
        {
            if (keys == null)
            {
                this._keys = new long[32];
                this._array = null;
            }
            else
            {
                this._keys = keys.clone();
                this._array = array.clone();
            }

            this.datas = new LinkedBlockingQueue<Short>();
        }

        @Override
        public boolean hasNext()
        {
            Short tmp = datas.poll();
            if (tmp != null)
            {
                data = tmp;
                return true;
            }
            else
            {
                key_loop_idx ++;
                while (key_idx < 32)
                {
                    this.high = _keys[key_idx] >> 32 & 4294967295l;
                    if (high != 0)
                    {
                        while (key_loop_idx < 32)
                        {
                            if ((high & 1l << key_loop_idx) != 0)
                            {
                                long long_data = _array[array_idx];
                                array_idx ++;
                                min = (short) (key_idx * 2048 + key_loop_idx * 64);
                                putData(long_data);
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
        public Short next()
        {
            return data;
        }

        private void putData(long long_data)
        {
            int bitCount = Long.bitCount(long_data);
            int findBit = 0;
            for (int i = 0; i < 64 && findBit < bitCount; i++)
            {
                if ( (long_data & 1l << i) != 0)
                {
                    findBit++;
                    datas.add((short) (min + i));
                }
            }
        }

        @Override
        public void remove() {
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        Iterator<Short> iterator = iterator();
        while (iterator.hasNext())
        {
            sb.append(iterator.next() & 0xFFFF);
            sb.append(",");
        }

        if (sb.length() > 1)
        {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("]");
        return sb.toString();
    }

    protected void loadData(ArrayContainer container)
    {
        this.keys = new long[32];
        long[] tmpArray = new long[1024];
        int last_update_k = 0;  // 0是不需要更新的
        int update_low = 0;

        for (int k = 0; k < container.cardinality; ++k)
        {
            int unsigned = toIntUnsigned(container.array[k]);

            int idx = unsigned >> 6;    // 获得在array第几个位置
            int idx_k = idx >> 5;       // 获得在idx在keys第几个位置
            int k_offset = idx - idx_k * 32;        // 获得在当前key的高位的位置
            long p_key = keys[idx_k];
            long n_key = p_key | 1l << (k_offset + 32);

            if (p_key != n_key)
            {
                keys[idx_k] = n_key;
                if (idx_k - last_update_k > 0)
                {
                    for (int i = last_update_k + 1 ; i <= idx_k && idx < 31 ; i ++)
                    {
                        keys[i] = keys[i] + update_low;
                    }
                    last_update_k = idx_k;
                }
                update_low ++;
            }

            long p = tmpArray[idx];
            long nval = p | 1l << (unsigned % 64);
            tmpArray[idx] = nval;

            this.cardinality ++;
        }

        for (int i = last_update_k + 1 ; i < 32 ; i ++)
        {
            keys[i] = keys[i] + update_low;
        }

        this.array = new long[update_low + 1];
        this.limit = (short) update_low;
        int array_idx = 0;
        for (long data : tmpArray)
        {
            if (data != 0)
            {
                this.array[array_idx++] = data;
            }
        }
    }

    @Override
    public DynScaleBitmapContainer clone()
    {
        return new DynScaleBitmapContainer(this.keys, this.array, this.cardinality);
    }

    public static void main(String[] args)
    {
        long start = System.currentTimeMillis();

        RoaringBitmap roaringBitmap = new RoaringBitmap();
        Container container = new ArrayContainer();
        Container container3 = new DynScaleBitmapContainer();
        org.roaringbitmap.Container arrayContainer = new org.roaringbitmap.ArrayContainer();

        Set<Integer> bitSet = new HashSet<Integer>();
        for (int i = 0; i < 10000; i++)
        {
            int word = (int) ((Math.random() * 65500));

            if (word == 0) {
                continue;
            }

            roaringBitmap.add(word);
            container = container.add((short) word);
            container3 = container3.add((short) word);
            arrayContainer = arrayContainer.add((short) word);
        }

        System.out.println(roaringBitmap.toString());
        System.out.println(container3.toString());
//        Container container2 =  container.toNextContainer();
//        System.out.println(container2.toString());
//        System.out.println(container.toString());
        System.out.println(arrayContainer.toString());

        for (Short s : arrayContainer)
        {
            if ( !container3.contain(s) )
            {
                System.out.println(s);
            }
        }

    }

}