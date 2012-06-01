package jd.controlling.packagecontroller;

public interface AbstractPackageChildrenNodeFilter<V extends AbstractPackageChildrenNode<?>> {

    public boolean isChildrenNodeFiltered(V node);

    /*
     * returns how many results we are interested in! <=0 = disabled/find as
     * many as possible
     */
    public int returnMaxResults();

}
