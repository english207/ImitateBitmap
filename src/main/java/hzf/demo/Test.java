package hzf.demo;

import hzf.demo.imitate.ImitateBitmap;
import org.roaringbitmap.ArrayContainer;
import org.roaringbitmap.Container;
import org.roaringbitmap.RoaringBitmap;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by hzf on 2018/6/15.
 *
 */
public class Test
{
    public static void main(String[] args)
    {
        ImitateBitmap bitmap = new ImitateBitmap();
        RoaringBitmap bitmap2 = new RoaringBitmap();
        RoaringBitmap bitmap3 = new RoaringBitmap();

        try
        {
            Thread.sleep(100);
        }
        catch (Exception e) { e.printStackTrace(); }


        Set<String> bitSet = new HashSet<String>();
        for (int i = 0; i < 1; i++)
        {
            int word = (int) (Math.random() * Integer.MAX_VALUE);
//            int word = (int) ((Math.random() * Short.MAX_VALUE * 20));
            bitSet.add(String.valueOf(word));
        }

        long start1 = System.nanoTime();
        for (String key : bitSet)
        {
            bitmap.add(Integer.valueOf(key));
        }
        long end1 = System.nanoTime();


        long start2 = System.nanoTime();
        for (String key : bitSet)
        {
            bitmap2.add(Integer.valueOf(key));
        }
        long end2 = System.nanoTime();

        System.out.println("cardinality1 - " + bitmap.cardinality());
        System.out.println("cardinality2 - " + bitmap2.getCardinality());
        System.out.println("bytes1 - " + bitmap.getSizeInBytes());
        System.out.println("bytes2 - " + bitmap2.getSizeInBytes());
        System.out.println("time1 - " + (end1 - start1) / 1000 /1000);
        System.out.println("time2 - " + (end2 - start2) / 1000 /1000);
        System.out.println("extend - " + bitmap.highContainer.time / 1000 /1000);
        System.out.println("howmanyDyn - " + bitmap.howmanyDyn());

        long start3 = System.nanoTime();
        boolean flag = true;
        for (String integer : bitSet)
        {
            flag = flag && bitmap.contain(Integer.valueOf(integer));
        }
        long end = System.nanoTime() - start3;

        System.out.println(flag + " - " + end / 1000 / 1000);

        start3 = System.nanoTime();
        flag = true;
        for (String integer : bitSet)
        {
            flag = flag && bitmap2.contains(Integer.valueOf(integer));
        }
        end = System.nanoTime() - start3;
        System.out.println(flag + " - " + end / 1000 / 1000);

        System.out.println(bitmap);
        System.out.println(bitmap2);

        System.out.println();

//        System.out.println(bitmap.or(bitmap2));
    }
}
