package jd.controlling.packagecontroller;

public interface AbstractNodeNotifier {

    public static enum NOTIFY {
        STRUCTURE_CHANCE,
        PROPERTY_CHANCE
    }

    void nodeUpdated(AbstractNode source, NOTIFY notify, Object param);

}
