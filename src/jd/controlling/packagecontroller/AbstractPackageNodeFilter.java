package jd.controlling.packagecontroller;

public interface AbstractPackageNodeFilter<V extends AbstractPackageNode<?, ?>> {

    public boolean acceptNode(V node);

    /*
     * returns how many results we are interested in! <=0 = disabled/find as many as possible
     */
    public int returnMaxResults();

}
