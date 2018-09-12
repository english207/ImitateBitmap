package hzf.demo.imitate;

import java.util.Iterator;

/**
 * Created by WTO on 2018/8/23 0023.
 *
 */
public class ImitateBitmap implements Iterable<Integer>
{
    public HighContainer highContainer = null;

    public ImitateBitmap()
    {
        this.highContainer = new DynHighContainer();
    }

    public ImitateBitmap(HighContainer hc)
    {
        if (hc == null)
        {
            throw new NullPointerException("HighContainer is null");
        }

        this.highContainer = hc;
    }

    public void add(int x)
    {
        this.highContainer = this.highContainer.add(x);
    }

    public boolean contain(int x)
    {
        return this.highContainer.contain(x);
    }

    public ImitateBitmap and(ImitateBitmap x)
    {
        HighContainer newHc = highContainer.and(x.highContainer);
        return new ImitateBitmap(newHc);
    }

    public ImitateBitmap or(ImitateBitmap x)
    {
        HighContainer newHc = highContainer.or(x.highContainer);
        return new ImitateBitmap(newHc);
    }

    public ImitateBitmap andNot(ImitateBitmap x)
    {
        HighContainer newHc = highContainer.andNot(x.highContainer);
        return new ImitateBitmap(newHc);
    }

    public Iterator<Integer> iterator()
    {
        return highContainer.iterator();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        Iterator<Integer> iterator = iterator();
        while (iterator.hasNext())
        {
            sb.append(iterator.next());
            sb.append(",");
        }

        if (sb.length() > 1)
        {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("]");
        return sb.toString();
    }
}
