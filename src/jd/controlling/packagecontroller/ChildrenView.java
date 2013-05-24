package jd.controlling.packagecontroller;

import java.util.List;

import org.jdownloader.DomainInfo;

public abstract class ChildrenView<T> {

    abstract public void update(List<T> items);

    abstract public void update();

    abstract public void requestUpdate();

    abstract public boolean updateRequired();

    abstract public DomainInfo[] getDomainInfos();

    abstract public void clear();

    abstract public List<T> getItems();

    abstract public boolean isEnabled();

}
