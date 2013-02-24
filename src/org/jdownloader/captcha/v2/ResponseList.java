package org.jdownloader.captcha.v2;

import java.util.ArrayList;

public class ResponseList<E> extends ArrayList<AbstractResponse<E>> implements Comparable<ResponseList<E>> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int               sum              = 0;

    public boolean add(AbstractResponse<E> e) {
        sum += e.getPriority();
        return super.add(e);

    }

    public int getSum() {
        return sum;
    }

    @Override
    public int compareTo(ResponseList<E> o) {
        return new Integer(sum).compareTo(new Integer(o.sum));
    }

    public E getValue() {
        return get(0).getValue();
    }

}
