package hzf.demo.imitate;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Created by hzf on 2018/7/9.
 *
 *      堆外内存bitmapContainer
 *      优点是大对象能减轻JVM GC压力
 *      缺点是性能相对来说会差一些
 */
public class DirectByteBufferContainer extends Container
{
    private ByteBuffer buffer = null;
    private int cardinality = 0;

    @Override
    public void add(short x)
    {
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect((32 + 4) * 8);
            buffer.limit((32 + 1) * 8);
        }

        int unsigned = toIntUnsigned(x);
        int idx = findIdx(unsigned);

        long p = array(idx);
        long nval = p | 1l << (unsigned % 64);
        array_update(idx, nval);
        cardinality += (p ^ nval) >>> x;
    }

    private long keys(int idx)
    {
        int _idx = idx * 8;
        return buffer.getLong(_idx);
    }

    private void keys_update(int idx, long data)
    {
        int _idx = idx * 8;
        buffer.putLong(_idx, data);
    }

    private long array(int idx)
    {
        int _idx = (32 + idx) * 8;
        return buffer.getLong(_idx);
    }

    private void array_update(int idx, long data)
    {
        int _idx = (32 + idx) * 8;
        buffer.putLong(_idx, data);
    }

    private int findIdx(int unsigned)
    {
        int idx = unsigned >> len;
        int idx_k = idx >> 5;
        long key = keys(idx_k);

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
        if (cardinality == 0) {
            return;
        }

        low += 32;
        low *= 8;
        int capacity = buffer.capacity();
        int limit = buffer.limit();
        if (low >= limit)        // 扩容
        {
            if (limit == capacity)
            {
                ByteBuffer newBuffer = ByteBuffer.allocateDirect(capacity + 4 * 8);
                ByteBuffer old = this.buffer;
                this.buffer.position(0);
                this.buffer = newBuffer.put(buffer);
                this.buffer.limit(limit + 8);
                clean(old);
            }
            else
            {
                this.buffer.limit(limit + 8);
            }

        }
        else if (low < limit)       // 从中间往后移动
        {
            if (limit == capacity)
            {
                this.buffer.position(0);
                this.buffer.limit(low);
                ByteBuffer buffer1 = buffer.slice();

                this.buffer.position(low);
                this.buffer.limit(limit);
                ByteBuffer buffer2 = buffer.slice();

                ByteBuffer newBuffer = ByteBuffer.allocateDirect(capacity + 4 * 8);
                newBuffer.put(buffer1);
                int pos = newBuffer.position();
                newBuffer.position(pos + 8);
                newBuffer.putLong(pos, 0);
                newBuffer.put(buffer2);

                this.buffer = newBuffer.put(buffer);
                this.buffer.flip();
            }
            else
            {
                this.buffer.position(low);
                this.buffer.limit(limit);
                ByteBuffer buffer2 = buffer.slice();
                this.buffer.position(low + 8);
                this.buffer.limit(limit + 8);
                this.buffer.put(buffer2);
                this.buffer.putLong(low, 0);

            }

        }
    }

    private void update(int idx_k, int update_offset) {
        update(idx_k, update_offset, 1);
    }

    private void update(int idx_k, int update_offset, int size)
    {
        long data = keys(idx_k) | 1l << update_offset;
        keys_update(idx_k, data);

        for (int i = idx_k + 1; i < 32; i++) {
            data = keys(i) + size;
            keys_update(i, data);
        }
    }

    @Override
    public void remove(short x) {

    }

    @Override
    public boolean contain(short x)
    {
        if (buffer == null) {
            return false;
        }
        int unsigned = toIntUnsigned(x);
        int idx = unsigned >> len;  // 除以 64
        int idx_k = idx >> 5;       // 除以 32
        long key = keys(idx_k);

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

        return (array(((int) low)) & 1l << (unsigned % 64)) != 0 ;
    }

    @Override
    public int cardinality() {
        return cardinality;
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
    public Iterator<Short> iterator() {
        return null;
    }

    private void clean(ByteBuffer byteBuffer)
    {
        //清除直接缓存
        ((DirectBuffer)byteBuffer).cleaner().clean();
    }

    public static void main(String[] args)
    {
        DirectByteBufferContainer container = new DirectByteBufferContainer();

        long start = System.nanoTime();
        container.add((short) 67);
        container.add((short) 5);
        container.add((short) 544);
        container.add((short) 5444);
        container.add((short) 13444);

        System.out.println(container.contain((short) 5));
        System.out.println(container.contain((short) 67));
        System.out.println(container.contain((short) 627));

        System.out.println(System.nanoTime() - start);

        DynScaleBitmapContainer container2 = new DynScaleBitmapContainer();

        start = System.nanoTime();
        container2.add((short) 67);
        container2.add((short) 5);

        System.out.println(container2.contain((short) 5));
        System.out.println(container2.contain((short) 67));
        System.out.println(container2.contain((short) 627));

        System.out.println(System.nanoTime() - start);

    }
}
