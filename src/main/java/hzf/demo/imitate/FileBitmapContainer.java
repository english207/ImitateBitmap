package hzf.demo.imitate;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

/**
 * Created by hzf on 2018/7/2.
 *
 *  以文件为存储的bitmap
 *
 */
public class FileBitmapContainer extends Container
{
    private boolean isEmpty = true;
    private int cardinality = 0;

    private static final int len = 6;
    private static int[] whereIdx = new int[32];
    static
    {
        int l = 0;
        for (int i = 0; i < 32; i++)
        {
            l |= l + ((1 << i - 1) & 2147483647);
            whereIdx[i] = l;
        }
    }

    private MappedByteBuffer mappedByteBuffer;
    public FileBitmapContainer()
    {
        try
        {
            RandomAccessFile rf = new RandomAccessFile("F:\\data\\data.txt", "rw");
            mappedByteBuffer = rf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, (1024 + 32) * 8);
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public Container add(short x)
    {
        if (isEmpty)
        {
            initFile(mappedByteBuffer);         // 等价于 new long[32]
            isEmpty = true;
        }

        int unsigned = toIntUnsigned(x);
        int idx = findIdx(unsigned);

        long p = array(idx);
        long nval = p | 1l << (unsigned % 64);
        array_update(idx, nval);
        cardinality += (p ^ nval) >>> x;

        return this;
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

    private void update(int idx_k, int update_offset) {
        update(idx_k, update_offset, 1);
    }

    private void update(int idx_k, int update_offset, int size)
    {
        long data = keys(idx_k);
        data |= 1l << update_offset;
        keys_update(idx_k, data);

        for (int i = idx_k + 1; i < 32; i++) {
            long dataTmp = keys(idx_k) + size;
            keys_update(i, dataTmp);
        }
    }

    private void initFile(MappedByteBuffer fileBuffer)
    {
        for (int i = 0; i < 32; i++)
        {
            fileBuffer.putLong(i * 8, 0);
        }
    }

    private void increaseCapacity(int low)
    {
//        if (isEmpty)
//        {
//            array = new long[1];
//            return;
//        }
//        if (low >= array.length)
//        {
//            this.array = Arrays.copyOf(array, low + 1);
//        }
//        else if (low < array.length)
//        {
//            this.array = Arrays.copyOf(array, array.length + 1);
//            System.arraycopy(array, low, array, low + 1, array.length - low - 1);
//            this.array[low] = 0;
//        }
    }


    @Override
    public Container remove(short x) {
        return this;
    }

    @Override
    public boolean contain(short x) {
        return false;
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
    public Container clone()
    {
        return null;
    }

    private long keys(int idx)
    {
        return readData(idx, mappedByteBuffer);
    }

    private long array(int idx)
    {
        return readData(idx + 32, mappedByteBuffer);
    }

    private void keys_update(int idx, long data)
    {
        putData(idx, data, mappedByteBuffer);
    }

    private void array_update(int idx, long data)
    {
        putData(idx + 32, data, mappedByteBuffer);
    }

    public long readData(int idx)
    {
        return readData(idx, mappedByteBuffer);
    }

    private long readData(int idx, MappedByteBuffer _mappedByteBuffer)
    {
        return  _mappedByteBuffer.getLong(idx * 8);
    }

    private void putData(int idx, long data, MappedByteBuffer _mappedByteBuffer)
    {
        _mappedByteBuffer.putLong(idx * 8, data);
    }


    public static void main(String[] args) {

//        FileBitmapContainer fileBitmap = new FileBitmapContainer();
//        System.out.println(fileBitmap.readData(16));
    }

    @Override
    public Iterator<Short> iterator() {
        return null;
    }
}
