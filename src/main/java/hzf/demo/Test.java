package hzf.demo;

import org.roaringbitmap.ArrayContainer;
import org.roaringbitmap.Container;

import java.util.BitSet;

/**
 * Created by huangzhenfeng on 2018/6/15.
 *
 */
public class Test
{
    public static void main(String[] args) {

        Container arrayContainer = new ArrayContainer();
        Container arrayContainer2 = new ArrayContainer();

        BitSet bitSet = new BitSet();
        BitSet bitSet2 = new BitSet();

        for (int i = 2568; i < 8908; i++) {
            arrayContainer = arrayContainer2.add((short) i);
            bitSet2.set(i);
        }

        for (int i = 0; i < 3568; i++) {
            arrayContainer = arrayContainer.add((short) i);
            bitSet.set(i);
        }

        Container arrayContainer3 = arrayContainer.or(arrayContainer2);
        bitSet.or(bitSet2);

        System.out.println(arrayContainer3.toString());
        System.out.println(bitSet.toString());

        System.out.println(arrayContainer3.getCardinality());
        System.out.println(bitSet.cardinality());


    }
}
