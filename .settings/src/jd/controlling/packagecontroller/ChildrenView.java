package jd.controlling.packagecontroller;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.DomainInfo;

public abstract class ChildrenView<T> {
    /**
	 * 
	 */
    private static final long serialVersionUID = 5324936310983343571L;

    abstract public void update(ArrayList<T> items);

    abstract public DomainInfo[] getDomainInfos();

    abstract public void clear();

    abstract public List<T> getItems();

    abstract public boolean isEnabled();

}
