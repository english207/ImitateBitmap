package hzf.demo.imitate;

import hzf.demo.imitate.utils.Search;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by hzf on 2018/9/12 0012.
 *
 *  参考RoaringBitmap中的ArrayContainer
 *
 */
public class DirectArrayContainer extends Container
{
    private ByteBuffer buffer = null;
    private int cardinality = 0;

    private static final int DEFAULT_MAX_SIZE = 4096;
    private static final int DEFAULT_INIT_SIZE = 4;
    private static final int SHOT_BYTES = 2;

    public DirectArrayContainer()
    {
        this.buffer = ByteBuffer.allocateDirect(4 * SHOT_BYTES);
        this.buffer.limit(SHOT_BYTES);
    }

    public DirectArrayContainer(final int capacity)
    {

    }


    protected short array(int idx)
    {
        int _idx = idx * SHOT_BYTES;
        return buffer.getShort(_idx);
    }

    protected void array_update(int idx, short data)
    {
        int _idx = idx * SHOT_BYTES;
        buffer.putShort(_idx, data);
    }

    @Override
    public Container add(short x)
    {
        int loc = Search.unsignedBinarySearchShort(buffer, 0, cardinality, x);
        if (loc < 0)
        {
            if (cardinality >= DEFAULT_MAX_SIZE)
            {
                Container container = toNextContainer();
                container = container.add(x);
                return container;
            }
            if (cardinality >= this.buffer.limit() * SHOT_BYTES)
            {
                increaseCapacity(false);
            }
            bufferMoveShort(buffer, -loc - 1, -loc, cardinality + loc + 1);
            array_update(-loc - 1, x);
            ++cardinality;
        }
        return this;
    }

    private void bufferMoveShort(ByteBuffer buffer, int srcPos, int tarPos, int len)
    {
        buffer.position(srcPos * SHOT_BYTES);
        buffer.limit((len + srcPos) * SHOT_BYTES);

        ByteBuffer buffer1 = buffer.slice();

        buffer.limit((cardinality + 1) * SHOT_BYTES);
        buffer.position(tarPos * SHOT_BYTES);
        buffer.put(buffer1);
    }

    private void increaseCapacity(boolean allowIllegalSize)
    {
        int len = this.buffer.limit() / SHOT_BYTES;
        int newCapacity = (len == 0) ? DEFAULT_INIT_SIZE
                : len < 64 ? len * 2
                : len < 1067 ? len * 3 / 2
                : len * 5 / 4;

        if (newCapacity > DEFAULT_MAX_SIZE && !allowIllegalSize)
        {
            newCapacity = DEFAULT_MAX_SIZE;
        }

        if (newCapacity > DEFAULT_MAX_SIZE - DEFAULT_MAX_SIZE / 16 && !allowIllegalSize)
        {
            newCapacity = DEFAULT_MAX_SIZE;
        }

        final ByteBuffer newContent = ByteBuffer.allocateDirect(newCapacity * SHOT_BYTES);
        this.buffer.rewind();
        newContent.put(this.buffer);
        this.buffer = newContent;
    }

    @Override
    public Container remove(short x) {
        return null;
    }

    @Override
    public boolean contain(short x)
    {
        return Search.unsignedBinarySearchShort(buffer, 0, cardinality, x) >= 0;
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
    public Container toNextContainer() {
        return null;
    }

    @Override
    public Container clone() {
        return null;
    }

    public void test()
    {
//        buffer.limit(4);


        int x = Search.unsignedBinarySearchShort(buffer, 0, 0, (short) 5);

        buffer.putShort(0, (short) 5);

        System.out.println(x);
        System.out.println(buffer.getShort(0));
        System.out.println(array(0));
        System.out.println(buffer);

        int src = 0;
        int len = 1;
        int tar = 1;

        int limit = buffer.limit();
        buffer.position(src * SHOT_BYTES);
        buffer.limit((len - src) * SHOT_BYTES);

        ByteBuffer buffer1 = buffer.slice();

        buffer.limit(2 * SHOT_BYTES);
        buffer.position(tar * SHOT_BYTES);

        buffer.put(buffer1);

        System.out.println(array(1));

    }

    public static void main(String[] args) {

        DirectArrayContainer container = new DirectArrayContainer();

//        container.test();


        container.add((short) 5);
        container.add((short) 2);
        container.add((short) 95);

        System.out.println(container.contain((short) 5));
        System.out.println(container.contain((short) 95));
        System.out.println(container.contain((short) 9));


        int[] array = new int[4];

        array[0] = 5;

        System.out.println(Arrays.toString(array));
        System.arraycopy(array, 0, array, 1, 1);
        System.out.println(Arrays.toString(array));

    }

    @Override
    public Iterator<Short> iterator() {
        return null;
    }
}
