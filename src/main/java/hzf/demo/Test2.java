package hzf.demo;

import hzf.demo.imitate.DynScaleBitmapContainer;
import hzf.demo.imitate.ImitateBitmap;
import org.roaringbitmap.ArrayContainer;
import org.roaringbitmap.Container;
import org.roaringbitmap.RoaringBitmap;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by hzf on 2018/6/15.
 *
 */
public class Test2
{
    public static void main(String[] args)
    {

        try
        {
            Thread.sleep(100);
        }
        catch (Exception e) { e.printStackTrace(); }

        Set<Integer> sets = new HashSet<Integer>();

        for (int i = 0; i < 1000; i++)
        {
            int word = Double.valueOf(Math.random() * 60550).intValue();
            sets.add(word);
        }

        long start = System.nanoTime();
        Container roaringContainer = new ArrayContainer();

        for (Integer integer : sets)
        {
            roaringContainer = roaringContainer.add(integer.shortValue());
        }
        System.out.println(roaringContainer.getSizeInBytes());
        System.out.println("roaring - " + (System.nanoTime() - start) / 1000 /1000  + " - ms");


        start = System.nanoTime();

        hzf.demo.imitate.Container container = new hzf.demo.imitate.ArrayContainer();
        for (Integer integer : sets)
        {
            container = container.add(integer.shortValue());
        }
        System.out.println(container.getSizeInBytes());
        System.out.println("myarray - " + (System.nanoTime() - start) / 1000 /1000 + " - ms");

        start = System.nanoTime();
        boolean flag = true;
        for (Integer integer : sets)
        {
            flag = flag && roaringContainer.contains(integer.shortValue());
        }


        start = System.nanoTime();
        flag = true;
        for (Integer integer : sets)
        {
            flag = flag && container.contain(integer.shortValue());
        }
        System.out.println((System.nanoTime() - start) / 1000  + " - ms");
        System.out.println((System.nanoTime() - start) / 1000  + " - ms");

    }
}
