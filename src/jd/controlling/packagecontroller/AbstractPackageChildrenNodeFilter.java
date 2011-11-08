package jd.controlling.packagecontroller;

public interface AbstractPackageChildrenNodeFilter<V extends AbstractPackageChildrenNode<?>> {

    public boolean isChildrenNodeFiltered(V node);

}
