package jd.controlling.packagecontroller;

public interface AbstractNodeNotifier<E extends AbstractNode> {

    public static enum NOTIFY {
        STRUCTURE_CHANCE,
        PROPERTY_CHANCE
    }

    void nodeUpdated(E source, NOTIFY notify);
}
