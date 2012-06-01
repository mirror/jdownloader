package jd.controlling.packagecontroller;

public interface AbstractNodeNotifier<E extends AbstractNode> {

    void nodeUpdated(E source);
}
