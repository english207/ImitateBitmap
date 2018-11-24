package hzf.demo.imitate.utils;

import java.nio.ByteBuffer;

/**
 * Created by WTO on 2018/11/16 0016.
 *
 */
public class Search
{
    public static final boolean USE_HYBRID_BINSEARCH = true;

    // copy from roaringbitmap
    public static int unsignedBinarySearch(final short[] array, final int begin, final int end, final short k)
    {
        int ikey = toIntUnsigned(k);
        // next line accelerates the possibly common case where the value would
        // be inserted at the end
        if ((end > 0) && (toIntUnsigned(array[end - 1]) < ikey))
        {
            return -end - 1;
        }
        int low = begin;
        int high = end - 1;
        // 32 in the next line matches the size of a cache line
        while (low + 32 <= high)
        {
            final int middleIndex = (low + high) >>> 1;
            final int middleValue = toIntUnsigned(array[middleIndex]);

            if (middleValue < ikey)
            {
                low = middleIndex + 1;
            }
            else if (middleValue > ikey)
            {
                high = middleIndex - 1;
            }
            else
            {
                return middleIndex;
            }
        }
        // we finish the job with a sequential search
        int x = low;
        for (; x <= high; ++x)
        {
            final int val = toIntUnsigned(array[x]);
            if (val >= ikey)
            {
                if (val == ikey)
                {
                    return x;
                }
                break;
            }
        }
        return -(x + 1);
    }

    // copy from roaringbitmap
    public static int unsignedBinarySearchShort(final ByteBuffer array, final int begin, final int end, final short k)
    {
        int ikey = toIntUnsigned(k);
        // next line accelerates the possibly common case where the value would
        // be inserted at the end
        if ((end > 0) && (toIntUnsigned(arrayShort(array, end - 1)) < ikey))
        {
            return -end - 1;
        }
        int low = begin;
        int high = end - 1;
        // 32 in the next line matches the size of a cache line
        while (low + 32 <= high)
        {
            final int middleIndex = (low + high) >>> 1;
            final int middleValue = toIntUnsigned(arrayShort(array, middleIndex));

            if (middleValue < ikey)
            {
                low = middleIndex + 1;
            }
            else if (middleValue > ikey)
            {
                high = middleIndex - 1;
            }
            else
            {
                return middleIndex;
            }
        }
        // we finish the job with a sequential search
        int x = low;
        for (; x <= high; ++x)
        {
            final int val = toIntUnsigned(arrayShort(array, x));
            if (val >= ikey)
            {
                if (val == ikey)
                {
                    return x;
                }
                break;
            }
        }
        return -(x + 1);
    }

    private static short arrayShort(ByteBuffer buffer, int idx)
    {
        int _idx = idx << 1;
        return buffer.getShort(_idx);
    }

    protected static int toIntUnsigned(short x) {
        return x & 0xFFFF;
    }

}
