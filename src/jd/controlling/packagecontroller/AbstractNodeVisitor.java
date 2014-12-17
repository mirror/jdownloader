package jd.controlling.packagecontroller;

public interface AbstractNodeVisitor<V extends AbstractPackageChildrenNode<E>, E extends AbstractPackageNode<V, E>> {

    /**
     * true= visit this package node
     * 
     * false = do not visit this package node
     * 
     * null = abort
     * 
     * @param pkg
     * @return
     */
    public Boolean visitPackageNode(E pkg);

    /**
     * true= visit next children node
     * 
     * false = visit next package node
     * 
     * null = abort
     * 
     * @param pkg
     * @return
     */
    public Boolean visitChildrenNode(V node);

}
