package hzf.demo.imitate;

import java.util.Iterator;

/**
 * Created by WTO on 2018/8/23 0023.
 *
 */
public class ImitateBitmap implements Iterable<Integer>
{
    private HighContainer highContainer = null;

    public ImitateBitmap()
    {
        this.highContainer = new DynHighContainer();
    }

    public void add(int x)
    {
        this.highContainer = this.highContainer.add(x);
    }

    public boolean contain(int x)
    {
        return this.highContainer.contain(x);
    }

    public Iterator<Integer> iterator()
    {
        return highContainer.iterator();
    }
}
