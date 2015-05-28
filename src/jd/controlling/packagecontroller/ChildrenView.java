package jd.controlling.packagecontroller;

import java.util.List;

import org.jdownloader.DomainInfo;

public abstract class ChildrenView<T> {

    public static enum ChildrenAvailablility {
        ONLINE,
        OFFLINE,
        MIXED,
        UNKNOWN;
    }

    abstract public ChildrenView<T> setItems(List<T> items);

    abstract public ChildrenView<T> aggregate();

    abstract public void requestUpdate();

    abstract public boolean updateRequired();

    abstract public DomainInfo[] getDomainInfos();

    abstract public void clear();

    abstract public List<T> getItems();

    abstract public boolean isEnabled();

    abstract public ChildrenAvailablility getAvailability();

    abstract public String getMessage(Object requestor);

}
