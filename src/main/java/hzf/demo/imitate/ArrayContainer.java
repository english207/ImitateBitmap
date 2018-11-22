package hzf.demo.imitate;

import hzf.demo.imitate.utils.Search;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by hzf on 2018/9/12 0012.
 *
 *  参考RoaringBitmap中的ArrayContainer
 *
 */
public class ArrayContainer extends Container
{
    short[] array = null;
    int cardinality = 0;

    private static final int DEFAULT_MAX_SIZE = 4096;
    private static final int DEFAULT_INIT_SIZE = 4;

    public ArrayContainer()
    {
        this.array = new short[4];
    }

    public ArrayContainer(final int capacity)
    {
        this.array = new short[capacity];
    }

    @Override
    public Container add(short x)
    {
        int loc = Search.unsignedBinarySearch(array, 0, cardinality, x);
        if (loc < 0)
        {
            if (cardinality >= DEFAULT_MAX_SIZE)
            {
                Container container = toNextContainer();
                container = container.add(x);
                return container;
            }
            if (cardinality >= this.array.length)
            {
                increaseCapacity(false);
            }
            System.arraycopy(array, -loc - 1, array, -loc, cardinality + loc + 1);
            array[-loc - 1] = x;
            ++cardinality;
        }
        return this;
    }

    private void increaseCapacity(boolean allowIllegalSize)
    {
        int newCapacity = (this.array.length == 0) ? DEFAULT_INIT_SIZE
                : this.array.length < 64 ? this.array.length * 2
                : this.array.length < 1067 ? this.array.length * 3 / 2
                : this.array.length * 5 / 4;

        if (newCapacity > DEFAULT_MAX_SIZE && !allowIllegalSize)
        {
            newCapacity = DEFAULT_MAX_SIZE;
        }

        if (newCapacity > DEFAULT_MAX_SIZE - DEFAULT_MAX_SIZE / 16 && !allowIllegalSize)
        {
            newCapacity = DEFAULT_MAX_SIZE;
        }
        this.array = Arrays.copyOf(this.array, newCapacity);
    }

    @Override
    public Container remove(short x) {
        return null;
    }

    @Override
    public boolean contain(short x) {
        return Search.unsignedBinarySearch(array, 0, cardinality, x) >= 0;
    }

    @Override
    public int cardinality() {
        return this.cardinality;
    }

    @Override
    public int getSizeInBytes() {
        return this.cardinality * 2 + 4;
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
        return new ArrayContainerIterator();
    }

    class ArrayContainerIterator implements Iterator<Short>
    {
        private int idx = 0;
        @Override
        public boolean hasNext() {
            return idx != cardinality;
        }

        @Override
        public Short next() {
            return array[idx++];
        }

        @Override
        public void remove() {

        }
    }

    @Override
    public String toString() {

        if (this.cardinality == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.cardinality - 1; i++) {
            sb.append(toIntUnsigned(this.array[i]));
            sb.append(",");
        }
        sb.append(toIntUnsigned(this.array[this.cardinality - 1]));
        sb.append("]");
        return sb.toString();
    }

    public Container toNextContainer()
    {
        int min1 = array[0];
        int max1 = array[cardinality - 1];

        DynScaleBitmapContainer dynScaleBitmapContainer = new DynScaleBitmapContainer();
        dynScaleBitmapContainer.loadData(this);
        return dynScaleBitmapContainer;
    }



    public static void main(String[] args) {

        Container container = new ArrayContainer();


        container.add((short) 55);
        container.add((short) 655);
        container.add((short) 455);
        container.add((short) 5555);

        container = container.toNextContainer();

        System.out.println(container);


    }
}
