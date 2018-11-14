package hzf.demo;

import hzf.demo.imitate.ImitateBitmap;
import org.roaringbitmap.ArrayContainer;
import org.roaringbitmap.Container;

import java.util.BitSet;
import java.util.Iterator;

/**
 * Created by hzf on 2018/6/15.
 *
 */
public class Test
{
    public static void main(String[] args)
    {
        ImitateBitmap bitmap = new ImitateBitmap();

        for (int i = 2568; i < 8908; i++)
        {
            bitmap.add(i);
        }

        Iterator<Integer> iterator = bitmap.iterator();

        while (iterator.hasNext())
        {
            System.out.println(iterator.next());
        }
    }
}
